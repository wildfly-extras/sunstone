package org.wildfly.extras.sunstone.api.impl.docker;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.docker.DockerApi;
import org.jclouds.docker.DockerApiMetadata;
import org.jclouds.docker.compute.functions.LoginPortForContainer;
import org.jclouds.docker.features.ContainerApi;
import org.jclouds.docker.features.ImageApi;
import org.jclouds.docker.features.MiscApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;

/**
 * Docker implementation of CloudProvider. This implementation uses JClouds internally.
 *
 */
public final class DockerCloudProvider extends AbstractJCloudsCloudProvider {
    public DockerCloudProvider(String providerName, Map<String, String> overrideMap) {
        super(providerName, CloudProviderType.DOCKER, overrideMap, DockerCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        // Create a Guice module that configures binds the function
        Module customLookupModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(LoginPortForContainer.class).to(NodeSshPortLookup.class).in(Scopes.SINGLETON);
            }
        };

        final ContextBuilder contextBuilder = ContextBuilder.newBuilder("docker")
                // REST API of docker daemon
                .endpoint(objectProperties.getProperty(Config.CloudProvider.Docker.ENDPOINT))
                // values are only valid for TLS protected endpoints (https://...), but still must be present for plain tcp (http://...)
                .credentials(
                        objectProperties.getProperty(Config.CloudProvider.Docker.TLS_CERT_PATH, Config.CloudProvider.Docker.TLS_CERT_PATH),
                        objectProperties.getProperty(Config.CloudProvider.Docker.TLS_KEY_PATH, Config.CloudProvider.Docker.TLS_KEY_PATH))
                .apiVersion(objectProperties.getProperty(Config.CloudProvider.Docker.API_VERSION, "1.21"))
                .modules(ImmutableSet.of(customLookupModule, new SLF4JLoggingModule(), new DynamicSshClientModule(), new SocketFinderOnlyPublicInterfacesModule()));
        final String caCertPath = objectProperties.getProperty(Config.CloudProvider.Docker.TLS_CA_CERT_PATH);
        if (!Strings.isNullOrEmpty(caCertPath)) {
            // if provided, configure the CA cert path in the ContextBuilder overrides
            Properties defaultPropertyOverrides = new Properties();
            defaultPropertyOverrides.setProperty(DockerApiMetadata.DOCKER_CA_CERT_PATH, caCertPath);
            contextBuilder.overrides(defaultPropertyOverrides);
        }
        return contextBuilder;
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new DockerNode(this, name, overrides);
    }

    /**
     * Returns JClouds Docker {@link ContainerApi} instance.
     */
    ContainerApi getContainerApi() {
        return getComputeServiceContext().unwrapApi(DockerApi.class).getContainerApi();
    }

    /**
     * Returns JClouds Docker {@link ImageApi} instance.
     */
    ImageApi getIMageApi() {
        return getComputeServiceContext().unwrapApi(DockerApi.class).getImageApi();
    }

    /**
     * Returns JClouds Docker {@link MiscApi} instance.
     */
    MiscApi getMiscApi() {
        return getComputeServiceContext().unwrapApi(DockerApi.class).getMiscApi();
    }

    /**
     * Do basic alignment with <a href="https://tools.ietf.org/html/rfc1035">RFC 1035</a>.
     */
    @Override
    protected String postProcessNodeGroupWhenCreatingNode(String nodeGroup) {
        // RFC 1035:
        // The labels must start with a letter, end with a letter or digit, and have as interior characters only letters, digits, and hyphen.
        // Labels must be 63 characters or less.
        // Comparisons between character strings (e.g., labels, domain names, etc.) are done in a case-insensitive manner.
        if (nodeGroup != null) {
            // convert to lower-case and remove characters, which are not allowed
            nodeGroup = nodeGroup.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9a-z\\-]", "");
            // remove leading digits and hyphens
            nodeGroup = nodeGroup.replaceAll("^[0-9\\-]+", "");
            // remove trailing hyphens (this is not very important as there will be the JClouds added suffix)
            nodeGroup = nodeGroup.replaceAll("-+$", "");
            // limit length to 59 characters (as 4 chars suffix is added by JClouds)
            if (nodeGroup.length()>59) {
                nodeGroup = nodeGroup.substring(0, 59);
            }
        }
        return nodeGroup;
    }
}
