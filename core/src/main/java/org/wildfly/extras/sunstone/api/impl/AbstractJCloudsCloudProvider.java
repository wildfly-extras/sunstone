package org.wildfly.extras.sunstone.api.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.util.OpenSocketFinder;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.ConfigProperties;
import org.wildfly.extras.sunstone.api.CreatedNodes;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

/**
 * Abstract {@link JCloudsCloudProvider} implementation which holds common logic.
 *
 */
public abstract class AbstractJCloudsCloudProvider implements JCloudsCloudProvider {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    protected final CloudProviderType cloudProviderType;
    protected final ObjectProperties objectProperties;
    protected final ComputeServiceContext computeServiceContext;
    protected final OpenSocketFinder socketFinder;
    protected final Injector guiceInjector;

    private final ConcurrentMap<String, JCloudsNode> nodes = new ConcurrentHashMap<>();

    /**
     * Constructor which takes name and map of overrides.
     *
     * @param name      provider name (must not be {@code null})
     * @param overrides configuration overrides (may be {@code null})
     * @throws NullPointerException when {@code name} is {@code null}
     */
    public AbstractJCloudsCloudProvider(String name, CloudProviderType cloudProviderType, Map<String, String> overrides,
                                        Function<ObjectProperties, ContextBuilder> contextBuilderCreator) {
        Objects.requireNonNull(name, "Cloud provider name has to be provided.");

        this.cloudProviderType = cloudProviderType;
        this.objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, name, overrides);

        LOGGER.debug("Creating {} ComputeServiceContext", cloudProviderType.getHumanReadableName());
        ContextBuilder contextBuilder = contextBuilderCreator.apply(objectProperties);
        // TODO the following builds a Guice injector twice, which is wasteful
        this.guiceInjector = contextBuilder.buildInjector();
        this.socketFinder = guiceInjector.getInstance(OpenSocketFinder.class);
        this.computeServiceContext = contextBuilder.buildView(ComputeServiceContext.class);
        LOGGER.info("Started {} cloud provider '{}'", cloudProviderType.getHumanReadableName(), name);
    }

    @Override
    public final String getName() {
        return objectProperties.getName();
    }

    @Override
    public final CloudProviderType getCloudProviderType() {
        return cloudProviderType;
    }

    @Override
    public final JCloudsNode createNode(String name) {
        return createNode(name, null);
    }

    @Override
    public final JCloudsNode createNode(String name, Map<String, String> overrides) {
        Objects.requireNonNull(name, "Node name has to be provided.");
        // the ConcurrentHashMap.compute method will block other threads trying to call it if there's a hash collision
        // (see its javadoc); an alternative solution that would avoid this problem would be to use a dummy value:
        //
        // old = nodes.putIfAbsent(name, DUMMY_VALUE)
        // if (old != null) ... node with this name already exists ...
        // nodes.put(name, createNodeInternal(name, overrides))
        JCloudsNode node = nodes.compute(name, (k, v) -> {
            if (v != null) {
                throw new IllegalArgumentException("There already exist node with given name \"" + k + "\"; "
                        + "You are not allowed to create two nodes with the same name under same provider");
            } else {
                final AbstractJCloudsNode<?> createdNode = (AbstractJCloudsNode<?>) createNodeInternal(name, overrides);
                LOGGER.debug("Node '{}' can be reached now on address {}", createdNode.getName(), createdNode.getPublicAddress());
                try {
                    createdNode.handleBootScript();
                    createdNode.waitForStartPorts(null);
                    LOGGER.debug("Node '{}' is succesfully started", createdNode.getName());
                } catch (Exception e) {
                    if (nodeRequiresDestroy()) {
                        computeServiceContext.getComputeService().destroyNode(createdNode.getInitialNodeMetadata().getId());
                    }
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException("Processing boot script failed for "
                                + cloudProviderType.getHumanReadableName() + " node '" + name + "'", e);
                    }
                }
                return createdNode;
            }
        });
        return node;
    }

    @Override
    public final CreatedNodes createNodes(String... nodeNames)
            throws NullPointerException, CompletionException, CancellationException {
        Objects.requireNonNull(nodeNames, "Node names have to be provided.");
        Arrays.stream(nodeNames).forEach(it -> Objects.requireNonNull(it, "Each node name must be not null"));
        CompletableFuture<Node>[] futures = Arrays.stream(nodeNames)
                .map(this::createNodeAsync)
                .toArray((IntFunction<CompletableFuture<Node>[]>) CompletableFuture[]::new);

        try {
            return new CreatedNodes(Arrays.stream(futures)
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.warn("Encountered exception while creating nodes => taking care of cleaning remaining nodes " +
                    "which might take a while please be patient");
            for (CompletableFuture<Node> future : futures) {
                if (!future.isCompletedExceptionally()) {
                    try {
                        @SuppressWarnings("resource")
                        Node node = future.join();
                        node.close();
                    } catch (Exception e2) {
                        e.addSuppressed(e2);
                    }
                }
            }
            throw e;
        }
    }

    @Override
    public CompletableFuture<Node> createNodeAsync(String name) {
        return createNodeAsync(name, null, ForkJoinPool.commonPool());
    }

    @Override
    public CompletableFuture<Node> createNodeAsync(String name, Executor executor) {
        return createNodeAsync(name, null, executor);
    }

    @Override
    public CompletableFuture<Node> createNodeAsync(String name, Map<String, String> overrides) {
        return createNodeAsync(name, overrides, ForkJoinPool.commonPool());
    }

    @Override
    public CompletableFuture<Node> createNodeAsync(String name, Map<String, String> overrides, Executor executor)
            throws NullPointerException {
        return CompletableFuture.supplyAsync(() -> createNode(name, overrides), executor);
    }

    protected abstract JCloudsNode createNodeInternal(String name, Map<String, String> overrides);

    @Override
    public final JCloudsNode getNode(String name) {
        Objects.requireNonNull(name, "Node name has to be provided.");
        return nodes.get(name);
    }

    @Override
    public final List<Node> getNodes() {
        return ImmutableList.copyOf(nodes.values());
    }

    @Override
    @Deprecated
    public final String getProperty(String propertyName, String defaultValue) {
        return objectProperties.getProperty(propertyName, defaultValue);
    }

    @Override
    public final ConfigProperties config() {
        return objectProperties;
    }

    final void destroyNode(JCloudsNode node) {
        LOGGER.info("Destroying {} node '{}'", cloudProviderType.getHumanReadableName(), node.getName());
        if (nodeRequiresDestroy()) {
            computeServiceContext.getComputeService().destroyNode(node.getInitialNodeMetadata().getId());
            LOGGER.info("Destroyed {} node '{}'", cloudProviderType.getHumanReadableName(), node.getName());
        } else {
            LOGGER.info("The {} node '{}' ({}) was configured to be kept running. The node is not destroyed.",
                    cloudProviderType.getHumanReadableName(), node.getName());
        }
        nodes.remove(node.getName());
    }

    @Override
    public final void close() {
        LOGGER.info("Destroying {} cloud provider '{}'", cloudProviderType.getHumanReadableName(), getName());

        for (Iterator<Map.Entry<String, JCloudsNode>> it = nodes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, JCloudsNode> nodeEntry = it.next();
            JCloudsNode node = nodeEntry.getValue();
            try {
                if (nodeRequiresDestroy()) {
                    computeServiceContext.getComputeService().destroyNode(node.getInitialNodeMetadata().getId());
                }
                it.remove();
                LOGGER.info("Destroyed {} node '{}'", cloudProviderType.getHumanReadableName(), node.getName());
            } catch (RuntimeException e) {
                LOGGER.error("Failed to destroy node '{}'", node.getName(), e);
            }
        }
        computeServiceContext.close();
        LOGGER.info("Destroyed {} cloud provider '{}'", cloudProviderType.getHumanReadableName(), getName());
    }

    // typically shouldn't be overridden
    public boolean nodeRequiresDestroy() {
        if (objectProperties.getPropertyAsBoolean(Config.LEAVE_NODES_RUNNING, false)) {
            return false;
        }

        return true;
    }

    @Override
    public final ComputeServiceContext getComputeServiceContext() {
        return computeServiceContext;
    }

    public final ObjectProperties getObjectProperties() {
        return objectProperties;
    }

    /**
     * Should be overridden if the cloud provider imposes some limits on the node group name. For example,
     * Azure imposes a length limit on the node group name, so the Azure implementation overrides this method
     * to make sure that the node group name isn't too long. Note that the {@code nodeGroup} param is only a prefix
     * that doesn't have to be unique. JClouds will add a unique suffix later on.
     */
    protected String postProcessNodeGroupWhenCreatingNode(String nodeGroup) {
        return nodeGroup;
    }

    public String getProviderSpecificPropertyName(ConfigProperties configProperties, String sharedName) {
        final String providerSpecificName = getCloudProviderType().getLabel() + "." + sharedName;
        return hasProviderSpecificPropertyName(configProperties, sharedName) ? providerSpecificName : sharedName;
    }

    public boolean hasProviderSpecificPropertyName(ConfigProperties configProperties, String sharedName) {
        final String providerSpecificName = getCloudProviderType().getLabel() + "." + sharedName;
        return configProperties.getProperty(providerSpecificName) != null;
    }
}
