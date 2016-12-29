package org.wildfly.extras.sunstone.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import org.wildfly.extras.sunstone.api.impl.azure.AzureCloudProvider;
import org.wildfly.extras.sunstone.api.impl.azurearm.AzureArmCloudProvider;
import org.wildfly.extras.sunstone.api.impl.baremetal.BareMetalCloudProvider;
import org.wildfly.extras.sunstone.api.impl.docker.DockerCloudProvider;
import org.wildfly.extras.sunstone.api.impl.ec2.EC2CloudProvider;
import org.wildfly.extras.sunstone.api.impl.openstack.OpenstackCloudProvider;

/**
 * A {@link Node} controller for one cloud. All cloud providers have a name, which must never be {@code null}.
 * All nodes created by a given cloud provider also have a name, which must never be {@code null}.
 *
 */
public interface CloudProvider extends AutoCloseable {
    /**
     * Returns name of this cloud provider.
     */
    String getName();

    /**
     * Returns cloud provider type.
     */
    CloudProviderType getCloudProviderType();

    /**
     * Creates and starts a new {@link Node} with given name.
     * If node with given name already exist an {@link IllegalArgumentException} is thrown.
     *
     * @throws NullPointerException when {@code name} is {@code null}
     * @throws IllegalArgumentException when there already exist node with given {@code name}
     */
    Node createNode(String name) throws NullPointerException;

    /**
     * Creates and starts a new {@link Node} with given name and configuration overrides.
     * If node with given name already exist an {@link IllegalArgumentException} is thrown.
     *
     * @throws NullPointerException when {@code name} is {@code null}
     * @throws IllegalArgumentException when there already exist node with given {@code name}
     */
    Node createNode(String name, Map<String, String> overrides) throws NullPointerException, IllegalArgumentException;

    /**
     * Creates and starts new {@link Node nodes} with given names. The nodes are started in parallel waiting for all
     * the nodes to be created and started.
     *
     * @throws NullPointerException when the {@code nodeNames} is {@code null}
     * @throws CompletionException when any of the nodes creation failed with exception
     * @throws CancellationException when there was canceled node creation or start up
     */
    CreatedNodes createNodes(String... nodeNames) throws NullPointerException, CompletionException, CancellationException;



    /**
     * Creates and starts a new {@link Node} with given name. The creation is done
     * asynchronously using default executor returning {@link CompletableFuture} for retrieving the result later on.
     *
     * @throws NullPointerException when {@code name} is {@code null}
     */
    CompletableFuture<Node> createNodeAsync(String name) throws NullPointerException;


    /**
     * The same as {@link #createNodeAsync(String)}, but using provided {@link Executor executor}
     *
     * @throws NullPointerException when {@code name} is {@code null}
     */
    CompletableFuture<Node> createNodeAsync(String name, Executor executor) throws NullPointerException;


    /**
     * Creates and starts a new {@link Node} with given name and configuration overrides. The creation is done
     * asynchronously using default executor returning {@link CompletableFuture} for retrieving the result later on.
     *
     * @throws NullPointerException when {@code name} is {@code null}
     */
    CompletableFuture<Node> createNodeAsync(String name, Map<String, String> overrides) throws NullPointerException;


    /**
     * The same as {@link #createNodeAsync(String, Map)}, but using provided {@link Executor executor}
     *
     * @throws NullPointerException when {@code name} is {@code null}
     */
    CompletableFuture<Node> createNodeAsync(String name, Map<String, String> overrides, Executor executor) throws NullPointerException;


    /**
     * Returns {@link Node} (created by this provider) with given name. Returns {@code null}
     * if a node with given name is not found.
     *
     * @throws NullPointerException when {@code name} is {@code null}
     */
    Node getNode(String name) throws NullPointerException;

    /**
     * Returns all {@link Node}s created by this provider.
     */
    List<Node> getNodes();

    /** Returns the {@link ConfigProperties configuration properties} of this cloud provider. */
    ConfigProperties config();

    /**
     * Destroys this cloud provider with all created {@link Node}s.
     */
    void close();

    // nice to have
    // List<String> getImageNames() throws OperationNotSupportedException;

    // ---

    /**
     * Creates a new {@link CloudProvider} with given name. The name refers to configuration provided.
     *
     * @param providerName provider name (must not be {@code null})
     * @throws NullPointerException when {@code providerName} is {@code null}
     */
    static CloudProvider create(String providerName) {
        return create(providerName, null);
    }

    /**
     * Creates a new {@link CloudProvider} with given name.  The name refers to configuration provided.
     * The {@code overrideMap} overrides the configuration properties.
     *
     * @param providerName provider name (must not be {@code null})
     * @param overrideMap configuration overrides (may be {@code null})
     * @throws NullPointerException when {@code providerName} is {@code null}
     */
    static CloudProvider create(String providerName, Map<String, String> overrideMap) {
        Objects.requireNonNull(providerName, "Cloud provider name has to be provided.");

        final ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, providerName, overrideMap);
        final CloudProviderType type = CloudProviderType.fromLabel(objectProperties.getProperty(Config.CloudProvider.TYPE));
        switch (type) {
            case DOCKER:
                return new DockerCloudProvider(providerName, overrideMap);
            case EC2:
                return new EC2CloudProvider(providerName, overrideMap);
            case AZURE:
                return new AzureCloudProvider(providerName, overrideMap);
            case AZURE_ARM:
                return new AzureArmCloudProvider(providerName, overrideMap);
            case OPENSTACK:
                return new OpenstackCloudProvider(providerName, overrideMap);
            case BARE_METAL:
                return new BareMetalCloudProvider(providerName, overrideMap);
            default:
                throw new IllegalArgumentException("Unknown cloud provider type: " + type);
        }
    }
}
