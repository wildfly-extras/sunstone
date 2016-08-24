package org.wildfly.extras.sunstone.api.impl.ec2;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.ec2.EC2Api;
import org.jclouds.ec2.features.InstanceApi;
import org.jclouds.ec2.features.KeyPairApi;
import org.jclouds.location.reference.LocationConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * EC2 implementation of CloudProvider. This implementation uses JClouds internally.
 *
 */
public final class EC2CloudProvider extends AbstractJCloudsCloudProvider {
    public EC2CloudProvider(String providerName, Map<String, String> overrideMap) {
        super(providerName, CloudProviderType.EC2, overrideMap, EC2CloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        Properties properties = new Properties();
        final String amiQuery = objectProperties.getProperty(Config.CloudProvider.EC2.AMI_QUERY);
        if (!Strings.isNullOrEmpty(amiQuery)) {
            properties.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, amiQuery);
        }
        final String ec2Regions = objectProperties.getProperty(Config.CloudProvider.EC2.REGION);
        if (!Strings.isNullOrEmpty(ec2Regions)) {
            properties.setProperty(LocationConstants.PROPERTY_REGIONS, ec2Regions);
        }

        Set<Module> modules = new HashSet<>();
        modules.add(new DynamicSshClientModule());
        modules.add(new SocketFinderOnlyPublicInterfacesModule());
        if (Boolean.parseBoolean(objectProperties.getProperty(Config.CloudProvider.EC2.LOG_EC2_OPERATIONS))) {
            // TODO we should add that unconditionally, like we do in all other cloud providers!
            // the reason why we don't [yet] is that the JClouds EC2 provider does some excessive logging,
            // e.g. the names of all available images, and we don't want our users to have to deal with that
            modules.add(new SLF4JLoggingModule());
        }

        final ContextBuilder contextBuilder = ContextBuilder.newBuilder("aws-ec2");
        final String endpoint = objectProperties.getProperty(Config.CloudProvider.EC2.ENDPOINT);
        if (!Strings.isNullOrEmpty(endpoint)) {
            contextBuilder.endpoint(endpoint);
        }
        contextBuilder
                .credentials(
                        objectProperties.getProperty(Config.CloudProvider.EC2.ACCESS_KEY_ID),
                        objectProperties.getProperty(Config.CloudProvider.EC2.SECRET_ACCESS_KEY)
                )
                .overrides(properties).modules(ImmutableSet.copyOf(modules));

        return contextBuilder;
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new EC2Node(this, name, overrides);
    }

    @Override
    protected String postProcessNodeGroupWhenCreatingNode(String nodeGroup) {
        // JClouds place a limitation of 63 characters on the node group (see https://github.com/wildfly-extras/sunstone/issues/27)
        // EC2 seems to accept longer instance names, so there's no problem with that; JClouds even append instance-ids to the node-group
        if (nodeGroup.length() > 63) {
            nodeGroup = nodeGroup.substring(0, 63);
        }

        return nodeGroup;
    }

    /**
     * @return instance API for the region defined in properties file
     */
    InstanceApi getInstanceAPI() {
        return getComputeServiceContext().unwrapApi(EC2Api.class)
                .getInstanceApiForRegion(getObjectProperties().getProperty(Config.CloudProvider.EC2.REGION)).get();
    }

    /**
     * @return key pair API for the region defined in properties file
     */
    KeyPairApi getKeyPairAPI() {
        return getComputeServiceContext().unwrapApi(EC2Api.class)
                .getKeyPairApiForRegion(getObjectProperties().getProperty(Config.CloudProvider.EC2.REGION)).get();
    }
}
