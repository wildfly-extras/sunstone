package org.wildfly.extras.sunstone.api.impl.openstack;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.base.Strings;
import com.google.inject.Module;

/**
 * Openstack implementation of CloudProvider. This implementation uses JClouds internally.
 */
public final class OpenstackCloudProvider extends AbstractJCloudsCloudProvider {
    public OpenstackCloudProvider(String providerName, Map<String, String> overrideMap) {
        super(providerName, CloudProviderType.OPENSTACK, overrideMap, OpenstackCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        Set<Module> modules = new HashSet<>();
        modules.add(new DynamicSshClientModule());
        modules.add(new SocketFinderOnlyPublicInterfacesModule());
        modules.add(new SLF4JLoggingModule());

        Properties overrides = new Properties();
        overrides.setProperty("jclouds.ssh.retry-auth", Boolean.FALSE.toString());
        overrides.setProperty("jclouds.ssh.max-retries", Integer.toString(1));

        ContextBuilder contextBuilder = ContextBuilder.newBuilder("openstack-nova");
        String endpoint = objectProperties.getProperty(Config.CloudProvider.Openstack.ENDPOINT);
        if (!Strings.isNullOrEmpty(endpoint)) {
            contextBuilder.endpoint(endpoint);
        }
        contextBuilder
                .credentials(
                        objectProperties.getProperty(Config.CloudProvider.Openstack.USERNAME),
                        objectProperties.getProperty(Config.CloudProvider.Openstack.PASSWORD)
                )
                .overrides(overrides)
                .modules(modules);

        return contextBuilder;
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new OpenstackNode(this, name, overrides);
    }

    /**
     * Returns JClouds {@link NovaApi} instance.
     */
    NovaApi getNovaApi() {
        return getComputeServiceContext().unwrapApi(NovaApi.class);
    }
}
