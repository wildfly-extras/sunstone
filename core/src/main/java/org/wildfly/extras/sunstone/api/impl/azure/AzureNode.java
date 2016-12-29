package org.wildfly.extras.sunstone.api.impl.azure;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.jclouds.azurecompute.compute.options.AzureComputeTemplateOptions;
import org.jclouds.azurecompute.domain.NetworkConfiguration;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsNode;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * Azure implementation of {@link org.wildfly.extras.sunstone.api.Node}. This implementation uses JClouds internally.
 *
 */
public class AzureNode extends AbstractJCloudsNode<AzureCloudProvider> {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private final String imageName;
    private final NodeMetadata initialNodeMetadata;

    public AzureNode(AzureCloudProvider azureCloudProvider, String name, Map<String, String> configOverrides) {
        super(azureCloudProvider, name, configOverrides);

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

        LOGGER.debug("Creating JClouds Template for node '{}' with options: {}", getName(), templateOptions);

        final OsFamily osFamily = objectProperties.getPropertyAsBoolean(Config.Node.Azure.IMAGE_IS_WINDOWS, false)
                ? OsFamily.WINDOWS
                : OsFamily.LINUX;
        Template template = computeService.templateBuilder()
                .imageId(image.getId())
                .hardwareId(size)
                .osFamily(osFamily)
                .options(templateOptions)
                .build();

        LOGGER.debug("Creating {} node '{}' from template: {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(),
                getName(), template);
        try {
            this.initialNodeMetadata = createNode(template);
        } catch (RunNodesException e) {
            throw new RuntimeException("Unable to create " + cloudProvider.getCloudProviderType().getHumanReadableName()
                    + " node from template " + template, e);
        }
        String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
        LOGGER.info("Started {} node '{}' from image {}, its public IP address is {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(), name, imageName, publicAddress);
    }

    private static AzureComputeTemplateOptions buildTemplateOptions(ObjectProperties objectProperties) {
        AzureComputeTemplateOptions templateOptions = new AzureComputeTemplateOptions();

        String loginName = objectProperties.getProperty(Config.Node.Azure.SSH_USER);
        if (Strings.isNullOrEmpty(loginName)) {
            throw new IllegalArgumentException("SSH user name for Azure virtual machine must be set");
        }
        templateOptions.overrideLoginUser(loginName);

        String loginPassword = objectProperties.getProperty(Config.Node.Azure.SSH_PASSWORD);
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
            final String nodeName = objectProperties.getName();
            LOGGER.debug("Looking up virtual network '{}' for node '{}'", virtualNetwork, nodeName);
            NetworkConfiguration.VirtualNetworkSite foundVirtualNetwork = cloudProvider.getVirtualNetworkApi()
                    .list()
                    .stream()
                    .filter(vn -> virtualNetwork.equals(vn.name()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Specified virtual network '" + virtualNetwork
                            + "' doesn't exist in Azure"));
            LOGGER.debug("Found virtual network '{}' for node '{}'", virtualNetwork, nodeName);

            LOGGER.debug("Looking up subnet '{}' in virtual network '{}' for node '{}'", subnet, virtualNetwork, nodeName);
            NetworkConfiguration.Subnet foundSubnet = foundVirtualNetwork.subnets()
                    .stream()
                    .filter(s -> subnet.equals(s.name()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Specified subnet '" + subnet
                            + "' doesn't exist in Azure virtual network '" + virtualNetwork + "'"));
            LOGGER.debug("Found subnet '{}' in virtual network '{}' for node '{}'", subnet, virtualNetwork, nodeName);

            LOGGER.debug("Using subnet '{}' ({}) in virtual network '{}' for node '{}'", subnet, foundSubnet.addressPrefix(),
                    virtualNetwork, nodeName);
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
