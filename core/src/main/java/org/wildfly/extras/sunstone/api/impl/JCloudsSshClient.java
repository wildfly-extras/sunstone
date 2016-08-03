package org.wildfly.extras.sunstone.api.impl;

import org.jclouds.compute.domain.ExecChannel;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.ssh.CommandExecution;
import org.wildfly.extras.sunstone.api.ssh.SshClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class JCloudsSshClient implements SshClient {
    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    private final org.jclouds.ssh.SshClient jclouds;

    /**
     * @throws OperationNotSupportedException when connecting to SSH fails, presumably because there's no SSH server
     * @throws InterruptedException when interrupted while waiting for SSH to connect
     */
    public JCloudsSshClient(String node, Supplier<org.jclouds.ssh.SshClient> jcloudsSshClientSupplier)
            throws OperationNotSupportedException, InterruptedException {

        org.jclouds.ssh.SshClient jcloudsSshClient = null;
        boolean connected = false;

        ExponentialBackoff backoff = new ExponentialBackoff(1000);
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        int count = 1;
        while (System.currentTimeMillis() < endTime) {
            jcloudsSshClient = jcloudsSshClientSupplier.get();

            if (jcloudsSshClient != null) {
                try {
                    jcloudsSshClient.connect();
                    connected = true;
                    break;
                } catch (Exception e) {
                    SunstoneCoreLogger.SSH.debug("Failed to establish SSH connection to node '{}' (attempt {})",
                            node, count, e);

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
            SunstoneCoreLogger.SSH.warn("Failed to establish SSH connection to node '{}'", node);
            throw new OperationNotSupportedException("Failed to establish SSH connection to node '" + node + "'");
        }

        ExecResponse sudoersResult = jcloudsSshClient.exec("sh -c '" + SshUtils.FileType.getShellTestStr("/etc/sudoers") + "'");
        SshUtils.FileType sudoersFile = SshUtils.FileType.fromExitCode(sudoersResult.getExitStatus());
        if (sudoersFile == SshUtils.FileType.FILE) {
            // see https://bugzilla.redhat.com/show_bug.cgi?id=1020147
            LOGGER.trace("Removing 'Defaults requiretty' from /etc/sudoers so that sudo works without a PTY");
            ExecResponse result = jcloudsSshClient.exec("sudo sed -i -e 's/Defaults    requiretty/#Defaults    requiretty/' /etc/sudoers");
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
}
