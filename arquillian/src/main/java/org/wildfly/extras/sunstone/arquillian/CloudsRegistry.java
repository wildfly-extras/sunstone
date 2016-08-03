package org.wildfly.extras.sunstone.arquillian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.config.descriptor.impl.ContainerDefImpl;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import org.wildfly.extras.sunstone.api.wildfly.WildFlyNode;

/**
 * Registry which holds references to managed cloud providers.
 * <p>
 * Note: If you use this class, don't use the controlled cloud provider instance calls directly! (i.e. closing, creating nodes,
 * etc)
 *
 */
public class CloudsRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudsRegistry.class);

    private final Map<String, CloudProvider> providerMap = new HashMap<String, CloudProvider>();

    /**
     * Returns {@link CloudProvider} instance with given name. If the instance doesn't exist yet, it's created. It can also
     * ensure that nodes with the given names are created in the returned provider.
     *
     * @param providerName
     * @param nodeNames
     * @return
     */
    public CloudProvider ensureProvider(String providerName, String... nodeNames) {
        synchronized (providerMap) {
            CloudProvider cp = providerMap.get(providerName);
            if (cp == null) {
                cp = CloudProvider.create(providerName);
                providerMap.put(providerName, cp);
            }
            for (String nodeName : nodeNames) {
                if (cp.getNode(nodeName) == null) {
                    cp.createNode(nodeName);
                }
            }
            return cp;
        }
    }

    /**
     * Returns existing provider with given name. If the provider doesn't exist <code>null</code> is returned.
     *
     * @param providerName
     * @return
     */
    public CloudProvider getProvider(String providerName) {
        synchronized (providerMap) {
            return providerMap.get(providerName);
        }
    }

    /**
     * Destroys the provider with given name.
     *
     * @param providerName
     */
    public void destroyProvider(String providerName) {
        CloudProvider cp = null;
        synchronized (providerMap) {
            cp = providerMap.remove(providerName);
        }
        if (cp != null) {
            cp.close();
        }
    }

    /**
     * Returns node with given name in given provider. If provider doesn't exist it's created. If the Node doesn't exist it's
     * created too.
     *
     * @param providerName
     * @param nodeName
     * @return
     */
    public Node ensureNode(String providerName, String nodeName) {
        synchronized (providerMap) {
            final CloudProvider cp = ensureProvider(providerName);
            final Node node = cp.getNode(nodeName);
            return node != null ? node : cp.createNode(nodeName);
        }
    }

    /**
     * Asynchronous version of {@link #ensureNode(String, String)} which returns {@link CompletableFuture} instance.
     * If provider doesn't exist it's created (synchronously). If the Node doesn't exist it's created too (asynchronously).
     *
     * @param providerName
     * @param nodeName
     * @return
     */
    public CompletableFuture<Node> ensureNodeAsync(String providerName, String nodeName) {
        synchronized (providerMap) {
            final CloudProvider cp = ensureProvider(providerName);
            final Node node = cp.getNode(nodeName);
            CompletableFuture<Node> result;
            if (node == null) {
                result = cp.createNodeAsync(nodeName);
            } else {
                result = new CompletableFuture<>();
                result.complete(node);
            }
            return result;
        }
    }

    /**
     * Returns node with given name in given provider. Compared to {@link #ensureNode(String, String)} method, this method does
     * not initialize neither provider nor the node. If the provider or node doesn't exist in the registry, then
     * <code>null</code> is returned.
     *
     * @param providerName
     * @param nodeName
     * @return
     */
    public Node getNode(String providerName, String nodeName) {
        synchronized (providerMap) {
            final CloudProvider cloudProvider = providerMap.get(providerName);
            return cloudProvider != null ? cloudProvider.getNode(nodeName) : null;
        }
    }

    /**
     * Returns {@link Set} of all nodes from all providers in the registry.
     *
     * @return
     */
    public Set<Node> getAllNodes() {
        synchronized (providerMap) {
            Set<Node> nodeSet = new HashSet<>();
            for (CloudProvider cp : providerMap.values()) {
                nodeSet.addAll(cp.getNodes());
            }
            return nodeSet;
        }
    }

    /**
     * Returns {@link Set} of all cloud provider names registered in this {@link CloudsRegistry}.
     *
     * @return
     */
    public Set<String> getCloudProviderNames() {
        synchronized (providerMap) {
            return new HashSet<>(providerMap.keySet());
        }
    }

    /**
     * Closes nodes (in all registered providers) which fits given predicate.
     *
     * @param predicate
     * @return
     */
    public int cleanupNodes(Predicate<Node> predicate) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        final List<RuntimeException> closeExceptions = new ArrayList<>();
        synchronized (providerMap) {
            final Map<String, CloudProvider> providerMapCopy = new HashMap<String, CloudProvider>(providerMap);
            for (CloudProvider provider : providerMapCopy.values()) {
                for (Node node : provider.getNodes()) {
                    if (predicate.test(node)) {
                        futures.add(CompletableFuture.runAsync(node::close));
                    }
                }
                if (provider.getNodes().isEmpty()) {
                    provider.close();
                    providerMap.remove(provider.getName());
                }
            }
            futures.forEach(f -> {
                try {
                    f.join();
                } catch (RuntimeException e) {
                    closeExceptions.add(e);
                }
            });
            for (CloudProvider provider : providerMapCopy.values()) {
                if (provider.getNodes().isEmpty()) {
                    provider.close();
                    providerMap.remove(provider.getName());
                }
            }
        }

        if (!closeExceptions.isEmpty()) {
            final RuntimeException firstException = closeExceptions.get(0);
            closeExceptions.subList(1, closeExceptions.size()).forEach(e -> firstException.addSuppressed(e));
            throw firstException;
        }

        return futures.size();
    }

    /**
     * Wrapper for providing WildFlyNode instance created around {@link #getNode(String, String)} method. It always returns a
     * new {@link WildFlyNode} instance or <code>null</code> when no such Node in given provider exists.
     *
     * @param providerName
     * @param nodeName
     * @return
     */
    public WildFlyNode wrapAsWildFlyNode(String providerName, String nodeName) {
        final Node node = getNode(providerName, nodeName);
        return node != null ? new WildFlyNode(node) : null;
    }

    public void configureWildflyContainer(final String node, ServiceLoader serviceLoader, ContainerRegistry registry,
            ContainerContext containerContext) throws IOException, Exception, LifecycleException {
        Container container = registry.getContainer(node);
        final ObjectProperties nodeProperties = getNodeProperties(node);
        final String provider = nodeProperties.getProperty(ArquillianConfig.Node.PROVIDER);
        WildFlyNode wflyNode = wrapAsWildFlyNode(provider, node);
        if (wflyNode != null) {
            wflyNode.waitUntilRunning();
            final int mgmtPort = wflyNode.getMgmtPort();
            final boolean containerIsDefault = nodeProperties.getPropertyAsBoolean(ArquillianConfig.Node.CONTAINER_IS_DEFAULT,
                    false);
            if (container == null) {
                ContainerDef definition = new ContainerDefImpl("whatever").setContainerName(wflyNode.getName())
                        .property("managementAddress", wflyNode.getPublicAddress())
                        .property("managementPort", String.valueOf(mgmtPort)).property("username", wflyNode.getMgmtUser())
                        .property("password", wflyNode.getMgmtPassword());
                if (containerIsDefault) {
                    definition.setDefault();
                }
                LOGGER.debug("Creating container {}", definition);
                container = registry.create(definition, serviceLoader);
            } else {
                container.getContainerConfiguration().overrideProperty("managementAddress", wflyNode.getPublicAddress())
                        .overrideProperty("managementPort", String.valueOf(mgmtPort))
                        .overrideProperty("username", wflyNode.getMgmtUser())
                        .overrideProperty("password", wflyNode.getMgmtPassword());
                if (containerIsDefault) {
                    container.getContainerConfiguration().setDefault();
                }
                LOGGER.debug("Updating container definition {}", container.getContainerConfiguration());
            }
            if (container.getState() != State.STARTED) {
                try {
                    containerContext.activate(container.getName());
                    container.setup();
                    container.start();
                } finally {
                    containerContext.deactivate();
                }
            }
        } else {
            throw new IllegalStateException("Unable to configure container for WildFly Node " + node
                    + " which doesn't exist in cloud provider " + provider);
        }
    }

    public void stopWildFlyContainerInRegistry(final String nodeName, ContainerRegistry registry,
            ContainerContext containerContext) throws LifecycleException {
        Container container = registry.getContainer(nodeName);
        if (container != null) {
            LOGGER.debug("Stoping container {}", nodeName);
            try {
                containerContext.activate(container.getName());
                container.stop();
            } finally {
                containerContext.deactivate();
            }
        } else {
            LOGGER.warn("Container {} was not found, so it can't be destroyed.", nodeName);
        }
    }

    private static ObjectProperties getNodeProperties(String node) {
        return new ObjectProperties(ObjectType.NODE, node);
    }
}
