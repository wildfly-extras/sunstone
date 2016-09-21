package org.wildfly.extras.sunstone.api.impl.docker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.shrinkwrap.impl.base.io.tar.TarInputStream;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.docker.DockerApi;
import org.jclouds.docker.compute.options.DockerTemplateOptions;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.Exec;
import org.jclouds.docker.domain.ExecCreateParams;
import org.jclouds.docker.domain.ExecInspect;
import org.jclouds.docker.domain.ExecStartParams;
import org.jclouds.docker.domain.HostConfig;
import org.jclouds.docker.domain.Resource;
import org.jclouds.docker.features.ImageApi;
import org.jclouds.docker.features.MiscApi;
import org.jclouds.docker.options.CreateImageOptions;
import org.jclouds.docker.util.DockerInputStream;
import org.jclouds.docker.util.StdStreamData;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.PortOpeningException;
import org.wildfly.extras.sunstone.api.PortOpeningTimeoutException;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsNode;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DefaultExecResult;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SshUtils;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Docker implementation of {@link org.wildfly.extras.sunstone.api.Node}. This implementation uses JClouds internally.
 *
 */
public class DockerNode extends AbstractJCloudsNode<DockerCloudProvider> {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private final String imageName;
    private final NodeMetadata initialNodeMetadata;
    private final DockerTemplateOptions templateOptions;

    public DockerNode(DockerCloudProvider dockerCloudProvider, String name, Map<String, String> configOverrides) {
        super(dockerCloudProvider, name, configOverrides, null);

        this.imageName = objectProperties.getProperty(Config.Node.Docker.IMAGE);
        if (Strings.isNullOrEmpty(imageName)) {
            throw new IllegalArgumentException("Docker image name was not provided for node " + name + " in cloud provider "
                    + dockerCloudProvider.getName());
        }

        final CreateImageOptions options = CreateImageOptions.Builder.fromImage(imageName);

        final ImageApi imageApi = computeServiceContext.unwrapApi(DockerApi.class).getImageApi();
        try (final InputStreamReader isr = new InputStreamReader(imageApi.createImage(options), "UTF-8")) {
            if (LOGGER.isDebugEnabled()) {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(isr);
                try {
                    reader.setLenient(true);
                    while (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        ProgressMessage message = gson.fromJson(reader, ProgressMessage.class);
                        if (message != null) {
                            LOGGER.debug("Docker daemon | {}", message);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Parsing JSON responses from Docker image create/pull failed.", e);
                }
            }
            char[] tmpBuff = new char[8 * 1024];
            // throw everything away
            while (isr.read(tmpBuff) > -1) {
                // just continue
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create/pull Docker image", e);
        }
        LOGGER.debug("Inspecting image {}", imageName);
        org.jclouds.docker.domain.Image image = imageApi.inspectImage(imageName);
        if (image == null) {
            throw new RuntimeException(
                    "Image '" + imageName + "' was not found. Check if pulling it from registry finished correctly.");
        }
        LOGGER.debug("Image {} has id {}", imageName, image.id());
        templateOptions = buildTemplateOptions(objectProperties);

        LOGGER.debug("Creating JClouds Template with options: {}", templateOptions);
        final Template template = computeService.templateBuilder().imageId(image.id()).options(templateOptions).build();

        LOGGER.debug("Creating {} node from template: {}", cloudProvider.getCloudProviderType().getHumanReadableName(),
                template);
        try {
            this.initialNodeMetadata = createNode(template);
            String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
            LOGGER.info("Started {} node '{}' from image {}, its public IP address is {}",
                    cloudProvider.getCloudProviderType().getHumanReadableName(), name, imageName, publicAddress);
        } catch (RunNodesException e) {
            throw new RuntimeException("Unable to create " + cloudProvider.getCloudProviderType().getHumanReadableName()
                    + " node from template " + template, e);
        }
    }

    private static DockerTemplateOptions buildTemplateOptions(ObjectProperties objectProperties) {
        org.jclouds.docker.domain.Config.Builder confBuilder = org.jclouds.docker.domain.Config.builder();
        HostConfig.Builder hostConfBuilder = HostConfig.builder().publishAllPorts(true);
        DockerTemplateOptions templateOptions = new DockerTemplateOptions();
        hostConfBuilder.networkMode(objectProperties.getProperty(Config.Node.Docker.NETWORK_MODE, "host"));

        final Map<String, List<Map<String, String>>> portBindingsConf = Maps.newHashMap();
        final String portBindings = objectProperties.getProperty(Config.Node.Docker.PORT_BINDINGS);
        if (!Strings.isNullOrEmpty(portBindings)) {
            Splitter.on(',').trimResults().omitEmptyStrings().splitToList(portBindings).stream().map(s -> s.split(":"))
                    .forEach(hostToNodeArr -> {
                        if (hostToNodeArr.length == 2) {
                            portBindingsConf.put(hostToNodeArr[1] + "/tcp", ImmutableList.<Map<String, String>> of(
                                    ImmutableMap.of("HostIp", "0.0.0.0", "HostPort", hostToNodeArr[0])));
                        } else {
                            LOGGER.warn("Port binding value has not correct format on Node '{}'", objectProperties.getName());
                        }
                    });
        }

        Map<String, Object> exposedPorts = null;
        final int[] inboundPorts = Pattern.compile(",")
                .splitAsStream(objectProperties.getProperty(Config.Node.Docker.INBOUND_PORTS, "")).mapToInt(Integer::parseInt)
                .toArray();
        if (inboundPorts.length > 0) {
            templateOptions.inboundPorts(inboundPorts);
            exposedPorts = Maps.newHashMap();
            for (int inboundPort : inboundPorts) {
                String portKey = inboundPort + "/tcp";
                exposedPorts.put(portKey, Maps.newHashMap());
                if (!portBindingsConf.containsKey(portKey)) {
                    portBindingsConf.put(portKey,
                            ImmutableList.<Map<String, String>> of(ImmutableMap.<String, String> of("HostIp", "0.0.0.0")));
                }
            }
            confBuilder.exposedPorts(exposedPorts);
        }

        if (!portBindingsConf.isEmpty()) {
            hostConfBuilder.portBindings(portBindingsConf);
        }

        final List<String> envConf = new ArrayList<>();

        final String env = objectProperties.getProperty(Config.Node.Docker.ENV);
        if (!Strings.isNullOrEmpty(env)) {
            envConf.addAll(Arrays.asList(env.split(objectProperties.getProperty(Config.Node.Docker.ENV_SPLIT_REGEX,
                    Config.Node.Docker.DEFAULT_ENV_SPLIT_REGEX))));
        }

        final int cpuShares = objectProperties.getPropertyAsInt(Config.Node.Docker.CPU_SHARES, -1);
        if (cpuShares > 0) {
            confBuilder.cpuShares(cpuShares);
        }

        final int memoryInMb = objectProperties.getPropertyAsInt(Config.Node.Docker.MEMORY_IN_MB, -1);
        if (memoryInMb > 0) {
            // set memory in bytes
            confBuilder.memory(memoryInMb * 1024 * 1024);
        }

        final String cmd = objectProperties.getProperty(Config.Node.Docker.CMD);
        if (cmd != null) {
            confBuilder.cmd(Arrays.asList(cmd.split(",", -1)));
        }

        final String entryPoint = objectProperties.getProperty(Config.Node.Docker.ENTRYPOINT);
        if (entryPoint != null) {
            confBuilder.entrypoint(Arrays.asList(entryPoint.split(",", -1)));
        }

        final String volumeBindings = objectProperties.getProperty(Config.Node.Docker.VOLUME_BINDINGS);
        if (volumeBindings != null) {
            hostConfBuilder.binds(Splitter.on(',').trimResults().omitEmptyStrings().splitToList(volumeBindings));
        }

        final String sshUser = objectProperties.getProperty(Config.Node.Docker.SSH_USER);
        if (!Strings.isNullOrEmpty(sshUser)) {
            templateOptions.overrideLoginUser(sshUser);
        }
        final String sshPass = objectProperties.getProperty(Config.Node.Docker.SSH_PASSWORD);
        if (!Strings.isNullOrEmpty(sshPass)) {
            templateOptions.overrideLoginPassword(sshPass);
        }
        final String sshPrivKey = objectProperties.getProperty(Config.Node.Docker.SSH_PRIVATE_KEY);
        if (!Strings.isNullOrEmpty(sshPrivKey)) {
            templateOptions.overrideLoginPrivateKey(sshPrivKey);
        }
        final String sshPort = objectProperties.getProperty(Config.Node.Docker.SSH_PORT);
        if (!Strings.isNullOrEmpty(sshPort)) {
            envConf.add(Config.Node.Docker.ENV_NAME_SSH_PORT + "=" + sshPort);
        }

        final String capAdd = objectProperties.getProperty(Config.Node.Docker.CAP_ADD);
        if (!Strings.isNullOrEmpty(capAdd)) {
            hostConfBuilder.capAdd(Arrays.asList(capAdd.split(",")));
        }
        final String privileged = objectProperties.getProperty(Config.Node.Docker.PRIVILEGED);
        if (!Strings.isNullOrEmpty(privileged)) {
            boolean bPrivileged = Boolean.parseBoolean(privileged);
            hostConfBuilder.privileged(bPrivileged);
        }

        confBuilder.env(envConf);
        confBuilder.hostConfig(hostConfBuilder.build());
        templateOptions.configBuilder(confBuilder);

        return templateOptions;
    }

    @Override
    public NodeMetadata getInitialNodeMetadata() {
        return initialNodeMetadata;
    }

    @Override
    public NodeMetadata getFreshNodeMetadata() {
        return computeService.getNodeMetadata(initialNodeMetadata.getId());
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    /**
     * Returns TCP port on Docker host for given internal port number.
     *
     * @see org.wildfly.extras.sunstone.api.Node#getPublicTcpPort(int)
     */
    @Override
    public int getPublicTcpPort(int tcpPort) {
        if (tcpPort < 0) {
            return tcpPort;
        }
        final HostConfig hostConfig = getContainer().hostConfig();
        if ("host".equals(hostConfig.networkMode())) {
            return tcpPort;
        } else {
            final String portName = tcpPort + "/tcp";
            int result = -1;
            final Map<String, List<Map<String, String>>> ports = hostConfig.portBindings();
            if (ports != null) {
                final List<Map<String, String>> portList = ports.get(portName);
                if (portList != null && !portList.isEmpty()) {
                    final String hostPortStr = portList.get(0).get("HostPort");
                    try {
                        result = Integer.parseInt(hostPortStr);
                    } catch (NumberFormatException e) {
                        LOGGER.debug(
                                "Unable to parse public port number for internal port {} (value found was {}) on node '{}'",
                                tcpPort, hostPortStr, getName());
                    }
                } else {
                    LOGGER.debug("Unable to find TCP port mapping for port {} on node '{}'", tcpPort, getName());
                }
            } else {
                LOGGER.debug("Port mapping is active, but no port mapping was found on node '{}'", getName());
            }
            LOGGER.debug("TCP port mapping: {} -> {} on node '{}'", tcpPort, result, getName());
            return result;
        }
    }

    @Override
    public boolean isPortOpen(int portNr) {
        int publicPortNr = getPublicTcpPort(portNr);
        if (publicPortNr == -1) {
            LOGGER.debug("Port {} has no TCP mapping on node '{}'", portNr, getName());
            return false;
        }
        final InetSocketAddress inetAddr = new InetSocketAddress(getPublicAddress(), publicPortNr);
        if (isSocketAddrOpen(inetAddr)) {
            // if we are not in host network mode and the tested interface is a loopback, then there is a problem with
            // "false-open" ports. Let's check it also on internal address.
            final Container container = getContainer();
            final HostConfig hostConfig = container.hostConfig();
            if (!"host".equals(hostConfig.networkMode())) {
                final InetAddress address = inetAddr.getAddress();
                if (address != null && address.isLoopbackAddress()) {
                    InetSocketAddress inetSockAddr2 = new InetSocketAddress(container.networkSettings().ipAddress(), portNr);
                    if (!isSocketAddrOpen(inetSockAddr2)) {
                        LOGGER.debug("Port {} is open on public interface, but not on private one ({}) on node '{}'",
                                publicPortNr, inetSockAddr2, getName());
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Inspects container and checks if the state==running.
     *
     * @see org.wildfly.extras.sunstone.api.Node#isRunning()
     */
    @Override
    public boolean isRunning() throws OperationNotSupportedException {
        return getContainer().state().running();
    }

    /**
     * Executes given command using Docker exec.
     */
    @Override
    public ExecResult exec(String... command) {
        MiscApi api = cloudProvider.getMiscApi();
        ExecCreateParams execCreateParams = ExecCreateParams.builder().attachStderr(true).attachStdout(true)
                .cmd(Arrays.asList(command)).build();
        Exec exec = api.execCreate(initialNodeMetadata.getId(), execCreateParams);

        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        try (DockerInputStream dis = new DockerInputStream(api.execStart(exec.id(), ExecStartParams.builder().build()))) {
            StdStreamData data = null;
            while (null != (data = dis.readStdStreamData())) {
                switch (data.getType()) {
                    case OUT:
                        baosOut.write(data.getPayload());
                        break;
                    case ERR:
                        baosErr.write(data.getPayload());
                        break;
                    default:
                        LOGGER.error("Unexpected STD stream type: {}", data.getType());
                        break;
                }
            }
            ExecInspect execInspect = api.execInspect(exec.id());
            return new DefaultExecResult(baosOut.toString(StandardCharsets.UTF_8.name()),
                    baosErr.toString(StandardCharsets.UTF_8.name()), execInspect.exitCode());
        } catch (IOException e) {
            throw new RuntimeException("Unable to complete Docker exec call", e);
        }
    }

    /**
     * Uses Docker stop to stop the node. This implementation doesn't use {@link Config.Node.Shared#STOP_TIMEOUT_SEC} property.
     *
     * @see org.wildfly.extras.sunstone.api.Node#stop()
     */
    @Override
    public void stop() {
        LOGGER.info("Stopping {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
        final String id = initialNodeMetadata.getId();
        LOGGER.debug("Stopping container {} (ID {})", getName(), id);
        cloudProvider.getContainerApi().stopContainer(id);
        LOGGER.info("Stopped {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
    }

    /**
     * Uses Docker start to (re)start the node. This implementation doesn't use {@link Config.Node.Shared#START_TIMEOUT_SEC}
     * property.
     *
     * @see org.wildfly.extras.sunstone.api.Node#start()
     */
    @Override
    public void start() {
        LOGGER.info("Starting {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
        final String id = initialNodeMetadata.getId();
        LOGGER.debug("Starting container {} (ID {})", getName(), id);
        cloudProvider.getContainerApi().startContainer(id);
        LOGGER.info("Started {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
    }

    /**
     * Uses Docker kill to kill the node.
     *
     * @see org.wildfly.extras.sunstone.api.Node#kill()
     */
    @Override
    public void kill() {
        LOGGER.info("Killing {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
        final String id = initialNodeMetadata.getId();
        LOGGER.debug("Killing container {} (ID {})", getName(), id);
        cloudProvider.getContainerApi().kill(id);
        LOGGER.info("Killed {} node '{}'", cloudProvider.getCloudProviderType().getHumanReadableName(), getName());
    }

    @Override
    public void copyFileFromNode(String remoteSrc, Path localTarget)
            throws OperationNotSupportedException, IllegalArgumentException, NullPointerException, FileNotFoundException {
        if (remoteSrc == null || localTarget == null) {
            throw new NullPointerException("Remote path and local path can't be null.");
        }
        if (Strings.isNullOrEmpty(remoteSrc)) {
            throw new IllegalArgumentException("Remote path must not be empty.");
        }

        SunstoneCoreLogger.SSH.debug("Copying remote path '{}' to local target '{}'", remoteSrc, localTarget);

        SshUtils.FileType remoteFileType = SshUtils.FileType
                .fromExitCode(exec("sh", "-c", SshUtils.FileType.getShellTestStr(remoteSrc)).getExitCode());
        if (remoteFileType == SshUtils.FileType.NA) {
            throw new FileNotFoundException("Source file " + remoteSrc + " doesn't exist in docker node " + getName());
        }

        SshUtils.FileType localFileType = SshUtils.FileType.fromPath(localTarget);
        if (localFileType == SshUtils.FileType.FILE && remoteFileType == SshUtils.FileType.DIRECTORY) {
            throw new IllegalArgumentException(
                    "Unable to copy remote directory " + remoteSrc + " to regular file " + localTarget);
        }

        try (TarInputStream tis = new TarInputStream(
                cloudProvider.getContainerApi().copy(initialNodeMetadata.getId(), Resource.create(remoteSrc)))) {
            if (remoteFileType == SshUtils.FileType.DIRECTORY) {
                SshUtils.untarFolder(tis, localTarget,
                        localFileType == SshUtils.FileType.NA ? new File(remoteSrc).getName() : null);
            } else {
                SshUtils.untarFile(tis, localTarget, localFileType == SshUtils.FileType.DIRECTORY);
            }

            while (tis.getNextEntry() != null) {
                // just skip
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Copying file " + remoteSrc + " from Docker container " + initialNodeMetadata.getId() + " failed", e);
        } finally {
            SunstoneCoreLogger.SSH.debug("Copied remote path '{}' to local target '{}'", remoteSrc, localTarget);
        }
    }

    // TODO: see if the implementation in AbstractJCloudsNode fits this use case
    @Override
    public void waitForPorts(long timeoutSeconds, int... portNrs) throws PortOpeningTimeoutException {
        if (portNrs == null || portNrs.length == 0)
            return;
        LOGGER.debug("Waiting for ports {} (timeout {}s)", Arrays.toString(portNrs), timeoutSeconds);
        List<Integer> missingPorts = Arrays.stream(portNrs).filter(i -> (getPublicTcpPort(i) == -1)).boxed()
                .collect(Collectors.toList());
        if (!missingPorts.isEmpty()) {
            throw new PortOpeningException(missingPorts.iterator().next(), String
                    .format("Ports %s of node %s have no mapping to public address.", missingPorts.toString(), getName()));
        }
        Set<Integer> publicPorts = IntStream.of(portNrs).boxed().collect(Collectors.toCollection(HashSet::new));
        long endTime = timeoutSeconds * 1000L + System.currentTimeMillis();
        do {
            for (Iterator<Integer> it = publicPorts.iterator(); it.hasNext();) {
                Integer port = it.next();
                if (isPortOpen(port)) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (System.currentTimeMillis() >= endTime || publicPorts.isEmpty()) {
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.debug("", e);
                }
            }
        } while (true);
        if (publicPorts.isEmpty()) {
            LOGGER.debug("All ports are open in given timeout");
        } else {
            throw new PortOpeningTimeoutException(publicPorts.iterator().next(),
                    String.format("Ports %s of node %s were not open in given timeout", publicPorts.toString(), getName()));
        }
    }

    /**
     * Returns Container for this node.
     */
    private Container getContainer() {
        return cloudProvider.getContainerApi().inspectContainer(initialNodeMetadata.getId());
    }

    /**
     * Checks if given socket address is open.
     */
    private boolean isSocketAddrOpen(InetSocketAddress inetSocketAddr) {
        try (SocketChannel sch = SocketChannel.open(inetSocketAddr)) {
            LOGGER.trace("Socket address {} is open.", inetSocketAddr);
            return true;
        } catch (IOException e) {
            LOGGER.trace("Socket address {} is not reachable", inetSocketAddr);
        }
        return false;
    }
}
