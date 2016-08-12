package org.wildfly.extras.sunstone.api.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.impl.base.io.tar.TarInputStream;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.util.OpenSocketFinder;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ConfigProperties;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.PortOpeningTimeoutException;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;
import org.wildfly.extras.sunstone.api.process.ExecBuilder;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * <p>Abstract class of a JClouds Node with some default implementations.</p>
 *
 * <p>Subclasses are generally expected to look like this:</p>
 *
 * <pre>
 * public class XxxNode extends AbstractJCloudsNode&lt;XxxCloudProvider> {
 *     private static final Logger LOGGER = LoggerFactory.getLogger(XxxNode.class);
 *
 *     private static final NodeConfigData XXX_NODE_CONFIG_DATA = new NodeConfigData(...);
 *
 *     private final String imageName;
 *     private final NodeMetadata initialNodeMetadata;
 *
 *     XxxNode(XxxCloudProvider xxxCloudProvider, String name, Map<String, String> configOverrides) {
 *         super(xxxCloudProvider, name, configOverrides, XXX_NODE_CONFIG_DATA);
 *
 *         ... read general configuration (image, hardware, location etc.) ...
 *         this.imageName = ...
 *
 *         XxxTemplateOptions templateOptions = buildTemplateOptions(objectProperties);
 *
 *         Template template = computeService.templateBuilder()
 *                 .imageId(...)
 *                 .xxx(...)
 *                 .options(templateOptions)
 *                 .build();
 *
 *         LOGGER.debug("Creating {} node from template: {}",
 *                 cloudProvider.getCloudProviderType().getHumanReadableName(), template);
 *         try {
 *             this.initialNodeMetadata = createNode(template);
 *             String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
 *             LOGGER.info("Started {} node '{}' from image {}, its public IP address is {}",
 *                     cloudProvider.getCloudProviderType().getHumanReadableName(), name, imageName, publicAddress);
 *         } catch (RunNodesException e) {
 *             throw new RuntimeException("Unable to create " + cloudProvider.getCloudProviderType().getHumanReadableName()
 *                     + " node from template " + template, e);
 *         }
 *
 *         try {
 *             waitForStartPorts();
 *         } catch (Exception e) {
 *             // to avoid leaking VMs in case there is an issue when opening the ports
 *             if (!objectProperties.getPropertyAsBoolean(Config.LEAVE_NODES_RUNNING, false)) {
 *                 computeService.destroyNode(initialNodeMetadata.getId());
 *             }
 *             throw e;
 *         }
 *     }
 *
 *     private static XxxTemplateOptions buildTemplateOptions(ObjectProperties objectProperties) {
 *         ... read specific configuration that can only be set via XxxTemplateOptions ...
 *     }
 *
 *     &#064;Override
 *     public NodeMetadata getInitialNodeMetadata() {
 *         return initialNodeMetadata;
 *     }
 *
 *     &#064;Override
 *     public NodeMetadata getFreshNodeMetadata() {
 *         return computeService.getNodeMetadata(initialNodeMetadata.getId());
 *     }
 *
 *     &#064;Override
 *     public String getImageName() {
 *         return imageName;
 *     }
 *
 *     ... other methods you need to implement or override ...
 * }
 * </pre>
 *
 */
public abstract class AbstractJCloudsNode<CP extends AbstractJCloudsCloudProvider> implements JCloudsNode {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private static final int SSH_CONNECTION_RETRIES = 12;
    private static final int SSH_CONNECTION_WAIT_BETWEEN_RETRIES = 5000; // milliseconds

    protected final CP cloudProvider;
    protected final ComputeServiceContext computeServiceContext;
    protected final ComputeService computeService;
    protected final ObjectProperties objectProperties;
    protected final OpenSocketFinder socketFinder;
    protected final NodeConfigData nodeConfigData;
    protected final String nodeGroupName;

    protected AbstractJCloudsNode(CP cloudProvider, String name, Map<String, String> configOverrides,
                                  NodeConfigData nodeConfigData) {
        this.cloudProvider = cloudProvider;
        this.computeServiceContext = cloudProvider.getComputeServiceContext();
        this.computeService = computeServiceContext.getComputeService();
        this.objectProperties = new ObjectProperties(ObjectType.NODE, name);
        this.objectProperties.applyOverrides(configOverrides);
        this.socketFinder = cloudProvider.socketFinder;
        this.nodeConfigData = nodeConfigData;

        String nodeGroup = NodeGroupUtil.nodeGroupName(objectProperties, cloudProvider.objectProperties);
        nodeGroup = cloudProvider.postProcessNodeGroupWhenCreatingNode(nodeGroup);
        this.nodeGroupName = nodeGroup;

        LOGGER.info("Starting {} node '{}'", cloudProvider.cloudProviderType.getHumanReadableName(), getName());
        LOGGER.debug("Using node group '{}' for node '{}'", nodeGroup, getName());

        // this constructor _intentionally_ doesn't start the node, this is left for subclass constructors
        //
        // trying to start the node here would be possible, but it requires calling instance methods
        // that might be overridden, which is always dangerous; the danger can be avoided, but only
        // at the cost of adding substantial new abstractions that are not very clear (believe me, I tried)
        //
        // in this case, less abstraction is better
    }

    /** Support method for subclasses that use it to actually start the node. */
    protected final NodeMetadata createNode(Template template) throws RunNodesException {
        return Iterables.getOnlyElement(computeService.createNodesInGroup(nodeGroupName, 1, template));
    }

    @Override
    public final String getName() {
        return objectProperties.getName();
    }

    @Override
    public final CP getCloudProvider() {
        return cloudProvider;
    }

    protected final void waitForStartPorts() {
        String portsString = objectProperties.getProperty(nodeConfigData.waitForPortsProperty, "");
        if (Strings.isNullOrEmpty(portsString)) {
            return;
        }

        int[] ports = Pattern.compile(",")
                .splitAsStream(portsString)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .mapToInt(Integer::parseInt)
                .toArray();
        int timeout = objectProperties.getPropertyAsInt(nodeConfigData.waitForPortsTimeoutProperty,
                nodeConfigData.waitForPortsDefaultTimeout);
        waitForPorts(timeout, ports);
    }

    @Override
    public void waitForPorts(long timeoutSeconds, int... portNrs) throws PortOpeningTimeoutException {
        LOGGER.debug("Waiting for ports {} with timeout {} sec on node '{}'", portNrs, timeoutSeconds, getName());
        NodeMetadata nodeMetadata = getFreshNodeMetadata();
        for (int port : portNrs) {
            try {
                socketFinder.findOpenSocketOnNode(nodeMetadata, port, timeoutSeconds, TimeUnit.SECONDS);
            } catch (NoSuchElementException e) {
                throw new PortOpeningTimeoutException(port);
            }
        }
    }

    @Override
    public ExecResult exec(String... command) throws OperationNotSupportedException, IOException, InterruptedException {
        return ExecBuilder.fromCommand(command).exec(this);
    }

    /**
     * Copies a file or a directory (TODO!) from local path to the node. If {@code remoteTarget}
     * is null, the current working directory on the remote machine is taken as a default
     * destination.
     *
     * @param localSrc a path to a file on the local machine that is to be copied
     * @param remoteTarget a path on the target machine where the file is to be copied to
     * @throws OperationNotSupportedException if this node implementation doesn't provide ssh access
     * @throws NullPointerException if {@code localSrc} is null ({@code remoteTarget} has a default)
     * @throws IllegalArgumentException if {@code localSrc} is not a regular file
     * @throws FileNotFoundException if {@code localSrc} does not exist
     */
    @Override
    public void copyFileToNode(Path localSrc, String remoteTarget)
            throws OperationNotSupportedException, IllegalArgumentException, NullPointerException, IOException,
            InterruptedException {
        if (localSrc == null) {
            throw new NullPointerException("Local path to copy file from can't be null.");
        }
        if (!Files.exists(localSrc)) {
            throw new FileNotFoundException("Local path to copy file from doesn't exist: " + localSrc);
        }
        if (!Files.isRegularFile(localSrc)) {
            throw new IllegalArgumentException(
                    "Local path to copy file from has to be a single regular file: " + localSrc);
        }

        SunstoneCoreLogger.SSH.debug("Copying local path '{}' to remote target '{}' on node '{}'", localSrc, remoteTarget,
                getName());

        if (Strings.isNullOrEmpty(remoteTarget)) {
            remoteTarget = exec("sh", "-c", "echo -n $PWD").getOutput();
        }
        SshUtils.FileType remoteFileType = SshUtils.FileType
                .fromExitCode(exec("sh", "-c", SshUtils.FileType.getShellTestStr(remoteTarget)).getExitCode());

        if (remoteFileType == SshUtils.FileType.DIRECTORY) {
            remoteTarget = remoteTarget + "/" + localSrc.getFileName();
        }

        SshClient sshClient = null;
        try {
            sshClient = getSsh();
            sshClient.put(remoteTarget, Payloads.newPayload(localSrc.toFile()));
            SunstoneCoreLogger.SSH.debug("Copied local path '{}' to remote target '{}' on node '{}'", localSrc, remoteTarget,
                    getName());
        } finally {
            if (sshClient != null) {
                sshClient.disconnect();
            }
        }
    }

    /**
     * Copies a remote file or folder to the local machine.
     *
     * @param remoteSrc path to remote file of folder
     * @param localTarget path to local folder, or, if {@code remoteSrc} is a file, also a file
     * @throws OperationNotSupportedException if this node implementation doesn't provide ssh access
     * @throws IllegalArgumentException if {@code remoteSrc} is a folder and {@code localTarget} is a regular file or if {@code remoteSrc} is an empty string
     * @throws NullPointerException if {@code remoteSrc} or {@code localTarget} is {@code null}.
     * @throws FileNotFoundException if {@code remoteSrc} does not exist
     */
    @Override
    public void copyFileFromNode(String remoteSrc, Path localTarget)
            throws OperationNotSupportedException, IllegalArgumentException, NullPointerException, IOException,
            InterruptedException {
        if (remoteSrc == null || localTarget == null) {
            throw new NullPointerException("Remote path and local path can't be null.");
        }
        if (Strings.isNullOrEmpty(remoteSrc)) {
            throw new IllegalArgumentException("Remote path must not be empty.");
        }

        SunstoneCoreLogger.SSH.debug("Copying remote path '{}' on node '{}' to local target '{}'", remoteSrc, getName(),
                localTarget);

        SshUtils.FileType remoteFileType = SshUtils.FileType
                .fromExitCode(exec("sh", "-c", SshUtils.FileType.getShellTestStr(remoteSrc)).getExitCode());
        if (remoteFileType == SshUtils.FileType.NA) {
            throw new FileNotFoundException("Source file " + remoteSrc + " doesn't exist in node " + getName());
        }

        SshUtils.FileType localFileType = SshUtils.FileType.fromPath(localTarget);
        if (localFileType == SshUtils.FileType.FILE && remoteFileType == SshUtils.FileType.DIRECTORY) {
            throw new IllegalArgumentException("Unable to copy remote directory " + remoteSrc + " to local regular file " + localTarget);
        }

        SshClient sshClient = getSsh();
        Path remoteFile = Paths.get(remoteSrc);
        String remoteTarSrc = remoteFile.getParent() + "/" + remoteFile.getFileName() + ".tar"; // TODO: separators for other platforms?
        String command = "cd " + remoteFile.getParent().toString() + " ; tar " + "cf " + remoteTarSrc + " " + remoteFile.getFileName().toString();
        SunstoneCoreLogger.SSH.debug("Using command '{}' to tar remote file on node '{}'", command, getName());
        ExecResponse execResponse = sshClient.exec(command);
        if (execResponse.getExitStatus() != 0) {
            SunstoneCoreLogger.SSH.warn("Error output when copying file on node '{}': {}", getName(), execResponse.getError());
            throw new IllegalStateException("File cannot be copied successfully. Return code of remote tar archive creation is " + execResponse.getExitStatus());
        }

        Payload payload = sshClient.get(remoteTarSrc);

        try (TarInputStream tis = new TarInputStream(payload.openStream())) {
            if (remoteFileType == SshUtils.FileType.DIRECTORY) {
                SshUtils.untarFolder(tis, localTarget, localFileType == SshUtils.FileType.NA ? new File(remoteSrc).getName() : null);
            } else {
                SshUtils.untarFile(tis, localTarget, localFileType == SshUtils.FileType.DIRECTORY);
            }

            while (tis.getNextEntry() != null) {
                // just skip
            }
        } catch (IOException e) {
            throw new RuntimeException("Copying file " + remoteSrc + " from node " + getInitialNodeMetadata().getId()
                    + " failed", e);
        } finally {
            exec("rm", remoteTarSrc); // TODO why not sshClient.exec(), like above for the 'tar' command?
            sshClient.disconnect();
            SunstoneCoreLogger.SSH.debug("Copied remote path '{}' on node '{}' to local target '{}'", remoteSrc, getName(),
                    localTarget);
        }
    }

    /**
     * Default implementation returning {@code true} if and only if JClouds {@link NodeMetadata#getStatus()}
     * returns {@code RUNNING}.
     */
    @Override
    public boolean isRunning() throws OperationNotSupportedException {
        return getFreshNodeMetadata().getStatus() == NodeMetadata.Status.RUNNING;
    }

    @Override
    public String getPublicAddress() {
        return Iterables.getFirst(getFreshNodeMetadata().getPublicAddresses(), null);
    }

    @Override
    public String getPrivateAddress() {
        return Iterables.getFirst(getFreshNodeMetadata().getPrivateAddresses(), null);
    }

    /**
     * Default implementation returning just the provided value (i.e., identity function).
     */
    @Override
    public int getPublicTcpPort(int tcpPort) {
        return tcpPort;
    }

    @Override
    public boolean isPortOpen(int portNr) {
        portNr = getPublicTcpPort(portNr);
        LOGGER.debug("Checking if port is open {} on node '{}'", portNr, getName());
        try {
            socketFinder.findOpenSocketOnNode(getFreshNodeMetadata(), portNr, 0, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public org.wildfly.extras.sunstone.api.ssh.SshClient ssh() throws InterruptedException {
        return new JCloudsSshClient(getName(), () -> {
            NodeMetadata nodeMetadata = getFreshNodeMetadata();
            return cloudProvider.getComputeServiceContext().utils().sshForNode().apply(nodeMetadata);
        });
    }

    /**
     * This method takes care of establishing a working ssh channel between the program and remote instance.
     * The reason for retrying the connection is that for some providers (EC2 for example) the authentication
     * fails for a short period of time (~30 sec) even after the ports are open.
     *
     * It is user's responsibility to close the sshClient properly ({@code sshClient.disconnect()})
     *
     * @throws IllegalStateException when the client could not be obtained or did not successfully connect
     */
    public SshClient getSsh() {
        SshClient sshClient = null;
        boolean connected = false;

        NodeMetadata nodeMetadata = getFreshNodeMetadata();

        for (int i = 0; i < SSH_CONNECTION_RETRIES; i++) {
            sshClient = cloudProvider.getComputeServiceContext().utils().sshForNode().apply(nodeMetadata);

            if (sshClient != null) {
                try {
                    sshClient.connect();
                    connected = true;
                    break;
                } catch (Exception e) {
                    SunstoneCoreLogger.SSH.debug("Failed to connect to SSH on node '{}' (attempt {} out of {})", getName(),
                            (i + 1), SSH_CONNECTION_RETRIES, e);
                    try {
                        sshClient.disconnect();
                    } catch (Exception e2) {
                        SunstoneCoreLogger.SSH.trace("Failed to destroy SSH client that failed to connect to node '{}'",
                                getName(), e2);
                    }

                    if (i + 1 >= SSH_CONNECTION_RETRIES) {
                        SunstoneCoreLogger.SSH.warn("Failed to obtain SSH connection for node '{}'", nodeMetadata.getHostname());
                    }
                }
            }

            try {
                Thread.sleep(SSH_CONNECTION_WAIT_BETWEEN_RETRIES);
            } catch (InterruptedException e) {
                SunstoneCoreLogger.SSH.warn("Interrupted while attempting to establish SSH connection to node '{}'", getName(),
                        e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (sshClient == null || !connected) {
            throw new IllegalStateException("The SSH client could not be obtained or connected. See logs for further information.");
        }

        return sshClient;
    }

    protected final boolean waitForState(NodeMetadata.Status targetStatus, long timeoutInSeconds) {
        Objects.requireNonNull(targetStatus, "targetStatus");
        LOGGER.debug("Waiting for status '{}' on node '{}' (timeout {}s)", targetStatus.name(), getName(), timeoutInSeconds);
        long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutInSeconds);
        while (System.currentTimeMillis() < endTime) {
            NodeMetadata.Status status = getFreshNodeMetadata().getStatus();
            if (status != null && status.equals(targetStatus)) {
                LOGGER.debug("Node '{}' is in status '{}'", getName(), targetStatus.name());
                return true;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for node '{}' to reach state '{}'", getName(), targetStatus.name(), e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        LOGGER.warn("Node '{}' didn't reach status '{}' in given time ({}s)", getName(), targetStatus.name(), timeoutInSeconds);
        return false;
    }

    @Override
    @Deprecated
    public final String getProperty(String propertyName, String defaultValue) {
        return objectProperties.getProperty(propertyName, defaultValue);
    }

    @Override
    public final ConfigProperties config() {
        return objectProperties;
    }

    @Override
    public final void close() {
        cloudProvider.destroyNode(this);
    }

    /**
     * Default JClouds implementation of the stop() method suspends this node.
     */
    public void stop() throws OperationNotSupportedException {
        LOGGER.info("Stopping {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
        computeService.suspendNode(getInitialNodeMetadata().getId());
        waitForState(NodeMetadata.Status.SUSPENDED, TimeUnit.MINUTES.toSeconds(2));
        LOGGER.info("Stopped {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
    }

    /**
     * Default JClouds implementation of the start() method resumes this node from suspend state.
     */
    public void start() throws OperationNotSupportedException {
        LOGGER.info("Starting {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
        computeService.resumeNode(getInitialNodeMetadata().getId());
        waitForState(NodeMetadata.Status.RUNNING, TimeUnit.MINUTES.toSeconds(5));
        LOGGER.info("Started {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
    }

    /**
     * Default JClouds implementation of the kill() method just calls the {@link #stop()} method.
     *
     * @see #stop()
     */
    public void kill() throws OperationNotSupportedException {
        LOGGER.debug("Killing node '{}' requested, but there's no specific implementation of 'kill'; stopping instead", getName());
        stop();
    }
}
