package org.wildfly.extras.sunstone.api.impl.azure;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.jclouds.ContextBuilder;
import org.jclouds.azurecompute.AzureComputeApi;
import org.jclouds.azurecompute.config.AzureComputeProperties;
import org.jclouds.azurecompute.domain.CloudService;
import org.jclouds.azurecompute.features.CloudServiceApi;
import org.jclouds.azurecompute.features.VirtualNetworkApi;
import org.jclouds.compute.functions.GroupNamingConvention;
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
 * Azure implementation of CloudProvider. This implementation uses JClouds internally.
 */
public final class AzureCloudProvider extends AbstractJCloudsCloudProvider {
    public AzureCloudProvider(String name, Map<String, String> overrides) throws NullPointerException {
        super(name, CloudProviderType.AZURE, overrides, AzureCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        String subscriptionId = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.Azure.SUBSCRIPTION_ID),
                "Azure subscription ID must be set");
        Path privateKeyFile = Objects.requireNonNull(
                objectProperties.getPropertyAsPath(Config.CloudProvider.Azure.PRIVATE_KEY_FILE, null),
                "Azure private key file must be set");
        String privateKeyPassword = Objects.requireNonNull(
                objectProperties.getProperty(Config.CloudProvider.Azure.PRIVATE_KEY_PASSWORD),
                "Azure private key password must be set");

        Properties defaultPropertyOverrides = new Properties();
        // default timeout of 1 minute is really too small for Azure; 5 minutes seems to be good enough
        defaultPropertyOverrides.setProperty(AzureComputeProperties.OPERATION_TIMEOUT, "" + (5 * 60000));
        // suspend shouldn't deallocate, that makes both suspend and resume very very slow
        defaultPropertyOverrides.setProperty(AzureComputeProperties.DEALLOCATE_WHEN_SUSPENDING, "false");

        return ContextBuilder.newBuilder("azurecompute")
                .endpoint("https://management.core.windows.net/" + subscriptionId)
                .credentials(privateKeyFile.toString(), privateKeyPassword)
                .overrides(defaultPropertyOverrides)
                .modules(ImmutableSet.of(
                        new SLF4JLoggingModule(),
                        new DynamicSshClientModule(),
                        new SocketFinderOnlyPublicInterfacesModule()
                ));
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new AzureNode(this, name, overrides);
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

    CloudServiceApi getCloudServiceApi() {
        return getComputeServiceContext().unwrapApi(AzureComputeApi.class).getCloudServiceApi();
    }

    VirtualNetworkApi getVirtualNetworkApi() {
        return getComputeServiceContext().unwrapApi(AzureComputeApi.class).getVirtualNetworkApi();
    }

    /**
     * The way JClouds generates unique names by default (see {@link org.jclouds.compute.strategy.impl.CreateNodesWithGroupEncodedIntoNameThenAddToSet#getNextNames})
     * is way too costly with the JClouds Azure provider. The reason is that it lists all nodes, which with Azure
     * translates to listing all cloud services and getting the deployment for each cloud service. In other words,
     * N+1 requests have to be made. This method is custom-tailored to the situation where the deployment has exactly
     * the same name as the containing cloud service, so 1 request is enough. That is much faster.
     */
    String generateUniqueNodeName(String nodeGroup) {
        Set<String> existingNames = getCloudServiceApi()
                .list()
                .stream()
                .map(CloudService::name)
                .collect(Collectors.toSet());
        GroupNamingConvention.Factory namingConvention = guiceInjector.getInstance(GroupNamingConvention.Factory.class);
        for (int i = 0; i < 100; i++) {
            String name = namingConvention.createWithoutPrefix().uniqueNameForGroup(nodeGroup);
            if (!existingNames.contains(name)) {
                return name;
            }
        }

        // shouldn't happen, hopefully...
        throw new IllegalStateException("Couldn't generate unique node name");
    }
}
