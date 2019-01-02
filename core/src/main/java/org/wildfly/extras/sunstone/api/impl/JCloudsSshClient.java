package org.wildfly.extras.sunstone.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.google.common.io.CharStreams;
import org.jclouds.compute.domain.ExecChannel;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.ssh.CommandExecution;
import org.wildfly.extras.sunstone.api.ssh.SshClient;

public final class JCloudsSshClient implements SshClient {
    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    private final org.jclouds.ssh.SshClient jclouds;

    /**
     * @throws OperationNotSupportedException when connecting to SSH fails, presumably because there's no SSH server
     * @throws InterruptedException when interrupted while waiting for SSH to connect
     */
    public JCloudsSshClient(AbstractJCloudsNode<?> node) throws OperationNotSupportedException, InterruptedException {

        org.jclouds.ssh.SshClient jcloudsSshClient = null;
        boolean connected = false;

        ExponentialBackoff backoff = new ExponentialBackoff(1000);
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        int count = 1;
        while (System.currentTimeMillis() < endTime) {
            NodeMetadata nodeMetadata = node.getFreshNodeMetadata();
            jcloudsSshClient = node.getCloudProvider().getComputeServiceContext().utils().sshForNode().apply(nodeMetadata);

            if (jcloudsSshClient != null) {
                try {
                    jcloudsSshClient.connect();
                    connected = true;
                    break;
                } catch (Exception e) {
                    SunstoneCoreLogger.SSH.debug("Failed to establish SSH connection to node '{}' (attempt {})", node.getName(),
                            count, e);

                    try {
                        jcloudsSshClient.disconnect();
                    } catch (Exception e2) {
                        SunstoneCoreLogger.SSH.trace("Failed to destroy SSH client that failed to connect", e2);
                    }
                }
            }

            backoff.delay();

            count++;
        }

        if (jcloudsSshClient == null || !connected) {
            SunstoneCoreLogger.SSH.warn("Failed to establish SSH connection to node '{}'", node.getName());
            throw new OperationNotSupportedException("Failed to establish SSH connection to node '" + node.getName() + "'");
        }

        if (isSudoersFixRequired(node, jcloudsSshClient)) {
            // see https://bugzilla.redhat.com/show_bug.cgi?id=1020147
            LOGGER.trace("Removing 'Defaults requiretty' from /etc/sudoers so that sudo works without a PTY");
            ExecResponse result = jcloudsSshClient
                    .exec("sudo -n sed -i -e 's/^Defaults[ ]\\+requiretty/#Defaults requiretty/' /etc/sudoers");
            if (result.getExitStatus() != 0) {
                LOGGER.warn("Failed removing 'Defaults requiretty' from /etc/sudoers, running with sudo might not work");
                LOGGER.debug("stdout: {}", result.getOutput());
                LOGGER.debug("stderr: {}", result.getError());
            }
        }

        this.jclouds = jcloudsSshClient;
    }

    @Override
    public CommandExecution exec(String command) {
        ExecChannel execChannel = jclouds.execChannel(command);
        return new JCloudsCommandExecution(execChannel);
    }

    @Override
    public ExecResult execAndWait(String command, long timeout, TimeUnit timeoutUnit) throws IOException, InterruptedException, TimeoutException {
        try (CommandExecution execution = exec(command)) {

            try {
                execution.await(timeout, timeoutUnit);
            } catch (TimeoutException timeoutElapsed) {

                LOGGER.warn("Command {} timed out after {} {}", command, timeout, timeoutUnit);

                String stdoutStr = null;
                try (InputStream stdout = execution.stdout()) {
                    stdoutStr = CharStreams.toString(new InputStreamReader(stdout, StandardCharsets.UTF_8));
                } catch (Exception err) {
                    LOGGER.warn("Failed to retrieve stdout for command {} : {}", command, err);
                }

                String stderrStr = null;
                try (InputStream stderr = execution.stderr()) {
                    stderrStr = CharStreams.toString(new InputStreamReader(stderr, StandardCharsets.UTF_8));
                } catch (Exception err) {
                    LOGGER.warn("Failed to retrieve stderr for command {} : {}", command, err);
                }

                throw new TimeoutException(
                        String.format("Command '%s' timed out %s stdout: %s %s stderr: %s",
                                command,
                                System.getProperty("line.separator"),
                                stdoutStr,
                                System.getProperty("line.separator"),
                                stderrStr)
                );
            }

            try (InputStream stdout = execution.stdout(); InputStream stderr = execution.stderr()) {
                return new DefaultExecResult(
                        CharStreams.toString(new InputStreamReader(stdout, StandardCharsets.UTF_8)),
                        CharStreams.toString(new InputStreamReader(stderr, StandardCharsets.UTF_8)),
                        execution.exitCode().orElse(-1)
                );
            }
        }
    }

    @Override
    public InputStream get(String path) throws IOException {
        Payload payload = jclouds.get(path);
        return payload.openStream();
    }

    @Override
    public void put(String path, Supplier<InputStream> data) {
        jclouds.put(path, Payloads.newInputStreamPayload(data.get()));
    }

    @Override
    public void close() {
        if (jclouds != null) {
            jclouds.disconnect();
        }
    }

    /**
     * Returns true if fixing /etc/sudoers (disabling "Defaults requiretty" configuration option) is required for given Node.
     *
     * @param node Node to check
     * @param jcloudsSshClient SSH client instance
     * @return true if fix is required by node configuration and "/etc/sudoers" file exists, false otherwise
     */
    private boolean isSudoersFixRequired(AbstractJCloudsNode<?> node, org.jclouds.ssh.SshClient jcloudsSshClient) {
        final String fixSudoersPropertyName = node.getCloudProvider().getProviderSpecificPropertyName(node.config(),
                Config.Node.Shared.SSH_FIX_SUDOERS);

        // check if fixing /etc/sudoers is requested in Node configuration
        if (node.config().getPropertyAsBoolean(fixSudoersPropertyName, false)) {
            // check if /etc/sudoers exists on the Node
            ExecResponse sudoersResult = jcloudsSshClient
                    .exec("sh -c '" + SshUtils.FileType.getShellTestStr("/etc/sudoers") + "'");
            SshUtils.FileType sudoersFile = SshUtils.FileType.fromExitCode(sudoersResult.getExitStatus());
            if (sudoersFile == SshUtils.FileType.FILE) {
                return true;
            }
        }
        return false;
    }

}
