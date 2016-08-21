package org.wildfly.extras.sunstone.api.impl.openstack;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIPPool;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPPoolApi;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsNode;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.NodeConfigData;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ResolvedImage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * OpenStack implementation of {@link org.wildfly.extras.sunstone.api.Node}. This implementation uses JClouds internally.
 *
 */
public class OpenstackNode extends AbstractJCloudsNode<OpenstackCloudProvider> {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;
    private static final String SPECIAL_VALUE_DEFAULT = "default";

    private static final NodeConfigData OPENSTACK_NODE_CONFIG_DATA = new NodeConfigData(
            Config.Node.Openstack.WAIT_FOR_PORTS,
            Config.Node.Openstack.WAIT_FOR_PORTS_TIMEOUT_SEC,
            300
    );

    private final String imageName;
    private final NodeMetadata initialNodeMetadata;

    public OpenstackNode(OpenstackCloudProvider osCloudProvider, String name, Map<String, String> configOverrides) {
        super(osCloudProvider, name, configOverrides, OPENSTACK_NODE_CONFIG_DATA);

        NovaTemplateOptions templateOptions = buildTemplateOptions(objectProperties);

        final TemplateBuilder templateBuilder = computeService.templateBuilder();

        final String instanceType = objectProperties.getProperty(Config.Node.Openstack.INSTANCE_TYPE);
        if (!Strings.isNullOrEmpty(instanceType)) {
            String hwId = null;
            Set<? extends Hardware> hws = computeService.listHardwareProfiles();
            for (Hardware hw : hws) {
                String hwName = hw.getName();
                if (instanceType.equals(hw.getId()) || instanceType.equals(hwName)
                        || (hwName != null && hwName.endsWith("/" + instanceType))) {
                    hwId = hw.getId();
                    break;
                }
            }
            if (hwId == null) {
                throw new IllegalArgumentException("Hardware configuration '" + instanceType + "' was not found.");
            }
            templateBuilder.hardwareId(hwId);
        }
        String region = objectProperties.getProperty(Config.Node.Openstack.REGION);
        if (Strings.isNullOrEmpty(region)) {
            LOGGER.debug("Region for node '{}' was not configured, trying to assign automatically", name);
            Set<? extends Location> locations = computeService.listAssignableLocations();
            if (locations.size() == 1) {
                region = locations.iterator().next().getId();
                LOGGER.debug("Automatically assigned region '{}' to node '{}'", region, name);
            } else {
                LOGGER.warn("Failed to automatically assign region to node '{}', use {} (too many regions found: {})",
                        name, Config.Node.Openstack.REGION, locations);
                throw new IllegalArgumentException("Region was not configured for node " + name);
            }
        }
        templateBuilder.locationId(region);

        ResolvedImage resolvedImage = ResolvedImage.fromNameAndId(
                objectProperties.getProperty(Config.Node.Openstack.IMAGE),
                objectProperties.getProperty(Config.Node.Openstack.IMAGE_ID),
                region, computeService
        );
        this.imageName = resolvedImage.humanReadableName;

        if (!templateOptions.getFloatingIpPoolNames().isPresent()) {
            LOGGER.debug("Floating IP pool for node '{}' was not configured, trying to assign automatically", name);
            final Optional<FloatingIPPoolApi> floatingIPPoolApi = osCloudProvider.getNovaApi().getFloatingIPPoolApi(region);
            if (floatingIPPoolApi.isPresent()) {
                final ImmutableList<String> pools = floatingIPPoolApi.get().list().transform(FloatingIPPool::getName).toList();
                LOGGER.debug("Automatically assigned floating IP pools to node '{}': {}", name, pools);
                templateOptions.floatingIpPoolNames(pools);
            } else {
                LOGGER.error("FloatingIPPoolApi missing, floating IP pool stays unconfigured, use {}",
                        Config.Node.Openstack.FLOATING_IP_POOLS);
            }
        }

        final Template template = templateBuilder.imageId(resolvedImage.fullId).options(templateOptions).build();

        LOGGER.debug("Creating {} node from template: {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(), template);
        try {
            this.initialNodeMetadata = createNode(template);
            String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
            LOGGER.info("Started {} node '{}' from image {}, its public IP address is {}",
                    cloudProvider.getCloudProviderType().getHumanReadableName(), name, imageName, publicAddress);
        } catch (RunNodesException e) {
            throw new RuntimeException("Unable to create " + cloudProvider.getCloudProviderType().getHumanReadableName()
                    + " node from template " + template, e);
        }

        try {
            waitForStartPorts();
        } catch (Exception e) {
            if (cloudProvider.nodeRequiresDestroy()) {
                computeService.destroyNode(initialNodeMetadata.getId());
            }
            throw e;
        }
    }

    private static NovaTemplateOptions buildTemplateOptions(ObjectProperties objectProperties) {
        NovaTemplateOptions templateOptions = NovaTemplateOptions.Builder.autoAssignFloatingIp(true);

        final String floatingIpPoolNames = objectProperties.getProperty(Config.Node.Openstack.FLOATING_IP_POOLS);
        if (!Strings.isNullOrEmpty(floatingIpPoolNames)) {
            final String[] poolArray = floatingIpPoolNames.split(",");
            if (poolArray.length > 0) {
                templateOptions.floatingIpPoolNames(poolArray);
            }
        }

        final int[] inboundPorts = Pattern.compile(",")
                .splitAsStream(objectProperties.getProperty(Config.Node.Openstack.INBOUND_PORTS, ""))
                .mapToInt(Integer::parseInt).toArray();
        if (inboundPorts.length > 0) {
            templateOptions.inboundPorts(inboundPorts);
        }

        templateOptions.runAsRoot(true);

        final String keyPair = objectProperties.getProperty(Config.Node.Openstack.KEY_PAIR);
        if (!Strings.isNullOrEmpty(keyPair)) {
            templateOptions.keyPairName(keyPair);
        }

        final String securityGroups = objectProperties.getProperty(Config.Node.Openstack.SECURITY_GROUPS);
        if (!Strings.isNullOrEmpty(securityGroups)) {
            templateOptions.securityGroups(securityGroups.split(","));
        }

        byte[] userData = null;
        final String userDataStr = objectProperties.getProperty(Config.Node.Openstack.USER_DATA);
        final Path userDataFile = objectProperties.getPropertyAsPath(Config.Node.Openstack.USER_DATA_FILE, null);
        if (!Strings.isNullOrEmpty(userDataStr)) {
            userData = userDataStr.getBytes(StandardCharsets.UTF_8);
        } else if (userDataFile != null) {
            if (Files.isReadable(userDataFile)) {
                try {
                    userData = Files.readAllBytes(userDataFile);
                } catch (IOException e) {
                    LOGGER.error("Unable to read Openstack User Data file", e);
                }
            } else {
                LOGGER.error("Openstack User Data file location is specified ({}), but it doesn't contain readable data.",
                        userDataFile);
            }
        }
        if (userData != null) {
            templateOptions.userData(userData);
        }

        LoginCredentials.Builder loginCredsBuilder = LoginCredentials.builder();
        final String userName = objectProperties.getProperty(Config.Node.Openstack.SSH_USER);
        if (!Strings.isNullOrEmpty(userName)) {
            loginCredsBuilder.user(userName);
        }

        final String privateKey = objectProperties.getProperty(Config.Node.Openstack.SSH_PRIVATE_KEY);
        if (!Strings.isNullOrEmpty(privateKey)) {
            loginCredsBuilder.privateKey(privateKey);
        } else {
            final Path pkFilePath = objectProperties.getPropertyAsPath(Config.Node.Openstack.SSH_PRIVATE_KEY_FILE, null);
            if (pkFilePath != null) {
                File pkFile = pkFilePath.toFile();
                if (!pkFile.exists() && SPECIAL_VALUE_DEFAULT.equals(pkFilePath.toString())) {
                    pkFile = new File(System.getProperty("user.home") + "/.ssh/id_rsa");
                }
                if (pkFile.canRead()) {
                    try {
                        loginCredsBuilder.privateKey(FileUtils.readFileToString(pkFile));
                    } catch (IOException e) {
                        throw new RuntimeException("Problem during reading private key from path " + pkFile, e);
                    }
                } else {
                    throw new IllegalArgumentException("Unable to read private key from path " + pkFilePath);
                }
            }
        }

        final String password = objectProperties.getProperty(Config.Node.Openstack.SSH_PASSWORD);
        if (!Strings.isNullOrEmpty(password)) {
            loginCredsBuilder.password(password);
        }

        templateOptions.overrideLoginCredentials(loginCredsBuilder.build());

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
}
