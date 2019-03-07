package org.wildfly.extras.sunstone.api.impl.openstack;

import com.google.common.base.Strings;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SocketFinderAllInterfacesModule;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPrivateInterfacesModule;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Openstack implementation of CloudProvider. This implementation uses JClouds internally.
 */
public final class OpenstackCloudProvider extends AbstractJCloudsCloudProvider {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    public OpenstackCloudProvider(String providerName, Map<String, String> overrideMap) {
        super(providerName, CloudProviderType.OPENSTACK, overrideMap, OpenstackCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        Set<Module> modules = new HashSet<>();
        modules.add(new DynamicSshClientModule());
        // OpenStack by default associates just a Private IP with every VM; optionally, also a Public IP can be associated: the so called 'Floating IP'
        String socketFinderAllowedInterfaces = objectProperties.getProperty(Config.CloudProvider.Openstack.SOCKET_FINDER_ALLOWED_INTERFACES);
        if (Strings.isNullOrEmpty(socketFinderAllowedInterfaces) || "PUBLIC".equalsIgnoreCase(socketFinderAllowedInterfaces)) {
            // Public IP: the so called 'Floating IP'
            modules.add(new SocketFinderOnlyPublicInterfacesModule());
        } else if ("PRIVATE".equalsIgnoreCase(socketFinderAllowedInterfaces)) {
            // Private IP: sometimes private address are routed externally and no floating IP (i.e. public address) is needed
            modules.add(new SocketFinderOnlyPrivateInterfacesModule());
        } else if ("ALL".equalsIgnoreCase(socketFinderAllowedInterfaces)) {
            modules.add(new SocketFinderAllInterfacesModule());
        }
        modules.add(new SLF4JLoggingModule());

        Properties overrides = new Properties();
        overrides.setProperty("jclouds.ssh.retry-auth", Boolean.FALSE.toString());
        overrides.setProperty("jclouds.ssh.max-retries", Integer.toString(1));

        String endpoint = objectProperties.getProperty(Config.CloudProvider.Openstack.ENDPOINT);
        if (!Strings.isNullOrEmpty(endpoint) && endpoint.contains("/v3")) {
            // ===================================================
            // Keystone V3
            // ===================================================
            LOGGER.info("Using {} version 3", KeystoneProperties.KEYSTONE_VERSION);
            overrides.put(KeystoneProperties.KEYSTONE_VERSION, "3");
            // Ignore SSL
            overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
            overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");

            // ===================================================
            // Using "Project-scoped" keystone v3 authentication:
            // https://jclouds.apache.org/guides/openstack/
            // ===================================================

            // Project name
            String projectName = objectProperties.getProperty(Config.CloudProvider.Openstack.PROJECT_NAME);
            if (projectName == null) {
                throw new IllegalStateException(String.format("Property '%s' not set", Config.CloudProvider.Openstack.PROJECT_NAME));
            } else {
                overrides.setProperty(KeystoneProperties.SCOPE, String.format("project:%s", projectName));
            }

            // Domain ID
            String projectDomainId = objectProperties.getProperty(Config.CloudProvider.Openstack.PROJECT_DOMAIN_ID);
            if (projectDomainId == null) {
                LOGGER.warn("Property {} not set", Config.CloudProvider.Openstack.PROJECT_DOMAIN_ID);
            } else {
                overrides.setProperty(KeystoneProperties.PROJECT_DOMAIN_ID, projectDomainId);
            }
        } else {
            // ===================================================
            // Keystone V2
            // ===================================================
            LOGGER.info("Using {} version 2", KeystoneProperties.KEYSTONE_VERSION);
        }

        ContextBuilder contextBuilder = null;

        if (!Strings.isNullOrEmpty(endpoint) && endpoint.contains("/v3")) {
            // ===================================================
            // Keystone V3
            // ===================================================
            String userDomainName = objectProperties.getProperty(Config.CloudProvider.Openstack.USER_DOMAIN_NAME);
            if (userDomainName == null) {
                throw new IllegalStateException(String.format("Property '%s' not set", Config.CloudProvider.Openstack.USER_DOMAIN_NAME));
            }
            contextBuilder = ContextBuilder.newBuilder("openstack-nova")
                    .endpoint(endpoint)
                    .modules(modules)
                    .overrides(overrides)
                    .credentials(
                            // env("OS_USER_DOMAIN_NAME") + ":" + env("OS_USERNAME"), env("OS_PASSWORD")
                            String.format("%s:%s", userDomainName, objectProperties.getProperty(Config.CloudProvider.Openstack.USERNAME)),
                            objectProperties.getProperty(Config.CloudProvider.Openstack.PASSWORD)
                    );
        } else {
            // ===================================================
            // Keystone V2
            // ===================================================
            contextBuilder = ContextBuilder.newBuilder("openstack-nova");
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
        }

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
