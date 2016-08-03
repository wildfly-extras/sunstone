package org.wildfly.extras.sunstone.api.impl.azure;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.jclouds.azurecompute.compute.options.AzureComputeTemplateOptions;
import org.jclouds.azurecompute.domain.NetworkConfiguration;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsNode;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.NodeConfigData;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.process.ExecBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Azure implementation of {@link org.wildfly.extras.sunstone.api.Node}. This implementation uses JClouds internally.
 *
 */
public class AzureNode extends AbstractJCloudsNode<AzureCloudProvider> {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private static final NodeConfigData AZURE_NODE_CONFIG_DATA = new NodeConfigData(
            Config.Node.Azure.WAIT_FOR_PORTS,
            Config.Node.Azure.WAIT_FOR_PORTS_TIMEOUT_SEC,
            600
    );

    private final String imageName;
    private final NodeMetadata initialNodeMetadata;

    public AzureNode(AzureCloudProvider azureCloudProvider, String name, Map<String, String> configOverrides) {
        super(azureCloudProvider, name, configOverrides, AZURE_NODE_CONFIG_DATA);

        this.imageName = objectProperties.getProperty(Config.Node.Azure.IMAGE);
        if (Strings.isNullOrEmpty(imageName)) {
            throw new IllegalArgumentException("Azure virtual machine image name was not provided for node " + name
                    + " in cloud provider " + azureCloudProvider.getName());
        }
        LOGGER.debug("Looking up virtual machine image '{}' for node '{}'", imageName, name);
        Image image = Objects.requireNonNull(computeService.getImage(imageName), "Image must exist: " + imageName);
        LOGGER.debug("Found virtual machine image '{}' for node '{}'", imageName, name);

        String size = objectProperties.getProperty(Config.Node.Azure.SIZE);
        if (size != null) {
            LOGGER.debug("Looking up virtual machine size '{}' for node '{}'", size, name);
            Set<? extends Hardware> hardwareProfiles = computeService.listHardwareProfiles();
            if (!hardwareProfiles.stream().map(Hardware::getId).anyMatch(size::equals)) {
                throw new IllegalArgumentException("Azure virtual machine size doesn't exist: " + size);
            }
            LOGGER.debug("Found virtual machine size '{}' for node '{}'", size, name);
        }

        AzureComputeTemplateOptions templateOptions = buildTemplateOptions(objectProperties);
        configureVirtualNetwork(objectProperties, templateOptions, cloudProvider);
        // the way JClouds generate unique names is too costly with Azure provider,
        // see the AzureCloudProvider.generateUniqueNodeName method javadoc for more details
        templateOptions.nodeNames(Collections.singleton(azureCloudProvider.generateUniqueNodeName(nodeGroupName)));

        LOGGER.debug("Creating JClouds Template with options: {}", templateOptions);

        final OsFamily osFamily = objectProperties.getPropertyAsBoolean(Config.Node.Azure.IMAGE_IS_WINDOWS, false)
                ? OsFamily.WINDOWS
                : OsFamily.LINUX;
        Template template = computeService.templateBuilder()
                .imageId(image.getId())
                .hardwareId(size)
                .osFamily(osFamily)
                .options(templateOptions)
                .build();

        LOGGER.debug("Creating {} node from template: {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(), template);
        try {
            this.initialNodeMetadata = createNode(template);
        } catch (RunNodesException e) {
            throw new RuntimeException("Unable to create " + cloudProvider.getCloudProviderType().getHumanReadableName()
                    + " node from template " + template, e);
        }
        String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
        LOGGER.info("Started {} node '{}' from image {}, its public IP address is {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(), name, imageName, publicAddress);

        int timeout = objectProperties.getPropertyAsInt(nodeConfigData.waitForPortsTimeoutProperty,
                nodeConfigData.waitForPortsDefaultTimeout);
        waitForPorts(timeout, 22);
        try {
            executeOnBootScript(objectProperties);
        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            this.close(); // no leaking!
            throw new RuntimeException("Unable to execute on boot script on node " + name, e);
        }

        waitForStartPorts();
    }

    private void executeOnBootScript(ObjectProperties objectProperties) throws IOException, InterruptedException {
        // only one can be passed at a time
        String script = objectProperties.getProperty(Config.Node.Azure.USER_DATA);
        Path scriptPath = objectProperties.getPropertyAsPath(Config.Node.Azure.USER_DATA_FILE, null);

        if (!Strings.isNullOrEmpty(script) && scriptPath != null) {
            throw new IllegalArgumentException("Only one of " + Config.Node.Azure.USER_DATA + " and " + Config.Node.Azure.USER_DATA_FILE + " can be specified");
        }
        if (!Strings.isNullOrEmpty(script)) {
            LOGGER.debug("The following script string will be run: '{}'", script);
            scriptPath = Files.createTempFile("tmpOnBootScript", ".sh");
            scriptPath.toFile().deleteOnExit();
            Files.write(scriptPath, script.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        }
        if (scriptPath != null) {
            LOGGER.debug("Script from file '{}' will be run", scriptPath);
            try {
                String remoteScriptPath = "/tmp/onBootScript.sh";
                this.copyFileToNode(scriptPath, remoteScriptPath);
                ExecResult result = ExecBuilder.fromCommand("sh", remoteScriptPath).withSudo().exec(this);
                LOGGER.trace("Execution result: {}: " + result);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error opening user data file " + scriptPath, e);
            }
        }
    }

    private static AzureComputeTemplateOptions buildTemplateOptions(ObjectProperties objectProperties) {
        AzureComputeTemplateOptions templateOptions = new AzureComputeTemplateOptions();

        String loginName = objectProperties.getProperty(Config.Node.Azure.SSH_USER);
        if (Strings.isNullOrEmpty(loginName)) {
            loginName = objectProperties.getProperty(Config.Node.Azure.LOGIN_NAME);
        }
        if (Strings.isNullOrEmpty(loginName)) {
            throw new IllegalArgumentException("SSH user name for Azure virtual machine must be set");
        }
        templateOptions.overrideLoginUser(loginName);

        String loginPassword = objectProperties.getProperty(Config.Node.Azure.SSH_PASSWORD);
        if (Strings.isNullOrEmpty(loginPassword)) {
            loginPassword = objectProperties.getProperty(Config.Node.Azure.LOGIN_PASSWORD);
        }
        if (Strings.isNullOrEmpty(loginPassword)) {
            throw new IllegalArgumentException("SSH password for Azure virtual machine must be set");
        }
        templateOptions.overrideLoginPassword(loginPassword);

        int[] inboundPorts = Pattern.compile(",")
                .splitAsStream(objectProperties.getProperty(Config.Node.Azure.INBOUND_PORTS, "22"))
                .filter(s -> !Strings.isNullOrEmpty(s))
                .mapToInt(Integer::parseInt)
                .toArray();
        templateOptions.inboundPorts(inboundPorts);

        String storageAccountName = objectProperties.getProperty(Config.Node.Azure.STORAGE_ACCOUNT_NAME);
        templateOptions.storageAccountName(storageAccountName);

        String provisionGuestAgent = objectProperties.getProperty(Config.Node.Azure.PROVISION_GUEST_AGENT);
        if (null != provisionGuestAgent) {
            templateOptions.provisionGuestAgent(Boolean.valueOf(provisionGuestAgent));
        }

        return templateOptions;
    }

    private static void configureVirtualNetwork(ObjectProperties objectProperties,
                                                AzureComputeTemplateOptions templateOptions,
                                                AzureCloudProvider cloudProvider) {

        String virtualNetwork = objectProperties.getProperty(Config.Node.Azure.VIRTUAL_NETWORK);
        String subnet = objectProperties.getProperty(Config.Node.Azure.SUBNET);

        if (virtualNetwork != null && subnet != null) {
            LOGGER.debug("Looking up virtual network '{}'", virtualNetwork);
            NetworkConfiguration.VirtualNetworkSite foundVirtualNetwork = cloudProvider.getVirtualNetworkApi()
                    .list()
                    .stream()
                    .filter(vn -> virtualNetwork.equals(vn.name()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Specified virtual network '" + virtualNetwork
                            + "' doesn't exist in Azure"));
            LOGGER.debug("Found virtual network '{}'", virtualNetwork);

            LOGGER.debug("Looking up subnet '{}' in virtual network '{}'", subnet, virtualNetwork);
            NetworkConfiguration.Subnet foundSubnet = foundVirtualNetwork.subnets()
                    .stream()
                    .filter(s -> subnet.equals(s.name()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Specified subnet '" + subnet
                            + "' doesn't exist in Azure virtual network '" + virtualNetwork + "'"));
            LOGGER.debug("Found subnet '{}' in virtual network '{}'", subnet, virtualNetwork);

            LOGGER.debug("Using subnet '" + subnet + "' (" + foundSubnet.addressPrefix()
                    + ") in virtual network '" + virtualNetwork + "'");
            templateOptions.virtualNetworkName(virtualNetwork);
            templateOptions.subnetNames(subnet);
        } else if (virtualNetwork != null) {
            throw new IllegalArgumentException("Virtual network '" + virtualNetwork
                    + "' specified, but no subnet specified; use " + Config.Node.Azure.SUBNET);
        } else if (subnet != null) {
            throw new IllegalArgumentException("Subnet '" + subnet
                    + "' specified, but there's no virtual network; use " + Config.Node.Azure.VIRTUAL_NETWORK);
        } else {
            // no virtual network nor subnet specified, that's OK
        }
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

    // TODO getPublicTcpPort() - does Azure even support port mappings? probably yes, but I don't know how currently
}
