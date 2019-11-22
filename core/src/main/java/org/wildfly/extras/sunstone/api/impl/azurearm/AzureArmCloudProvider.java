package org.wildfly.extras.sunstone.api.impl.azurearm;

import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.azurecompute.arm.config.AzureComputeProperties;
import org.jclouds.location.reference.LocationConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.Constants;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.collect.ImmutableSet;

/**
 * Azure ARM implementation of CloudProvider. This implementation uses JClouds internally.
 */
public class AzureArmCloudProvider extends AbstractJCloudsCloudProvider {
    public AzureArmCloudProvider(String name, Map<String, String> overrides) {
        super(name, CloudProviderType.AZURE_ARM, overrides, AzureArmCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        String subscriptionId = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.AzureArm.SUBSCRIPTION_ID),
                "Azure subscription ID must be set");
        String tenantId = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.AzureArm.TENANT_ID),
                "Azure tenant ID must be set");
        String applicationId = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.AzureArm.APPLICATION_ID, null),
                "Azure application ID must be set");
        String password = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.AzureArm.PASSWORD),
                "Azure password must be set");
        String location = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.AzureArm.LOCATION),
                "Azure location must be set");
        String publishers = objectProperties.getProperty(Config.CloudProvider.AzureArm.PUBLISHERS, "Canonical,RedHat");

        Properties defaultPropertyOverrides = new Properties();
        defaultPropertyOverrides.setProperty(AzureArmPropertiesUnsupported.OAUTH_ENDPOINT, "https://login.microsoftonline.com/" + tenantId + "/oauth2/token");
        defaultPropertyOverrides.setProperty(AzureComputeProperties.IMAGE_PUBLISHERS, publishers);
        defaultPropertyOverrides.put(LocationConstants.PROPERTY_REGIONS, location);
        // listing all images is very expensive, so they should be cached for a long time
        // unfortunately, the default is 1 minute, which can never be sufficient; we use 5 hours here
        defaultPropertyOverrides.setProperty(PROPERTY_SESSION_INTERVAL, "" + (5 * 60 * 60));

        return ContextBuilder.newBuilder("azurecompute-arm")
                .endpoint("https://management.azure.com/subscriptions/" + subscriptionId)
                .credentials(applicationId, password)
                .overrides(defaultPropertyOverrides)
                .modules(ImmutableSet.of(
                        new SLF4JLoggingModule(),
                        new DynamicSshClientModule(),
                        new SocketFinderOnlyPublicInterfacesModule()
                ));
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new AzureArmNode(this, name, overrides);
    }

    @Override
    protected String postProcessNodeGroupWhenCreatingNode(String nodeGroup) {
        // Azure limits the length of cloud service / VM name to 15 characters
        // JClouds will add 4 characters at the end, so we have up to 11 characters to our disposal

        if (nodeGroup.length() > 11 && nodeGroup.startsWith(Constants.SUNSTONE_PREFIX)) {
            nodeGroup = nodeGroup.substring(Constants.SUNSTONE_PREFIX.length());
        }

        if (nodeGroup.length() > 11) {
            nodeGroup = nodeGroup.substring(0, 11);
        }

        return nodeGroup;
    }
}
