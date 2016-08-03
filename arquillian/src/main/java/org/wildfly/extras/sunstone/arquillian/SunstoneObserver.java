package org.wildfly.extras.sunstone.arquillian;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.event.StartClassContainers;
import org.jboss.arquillian.container.spi.event.StopClassContainers;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.annotations.WithNode;
import org.wildfly.extras.sunstone.annotations.WithWildFlyContainer;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

import com.google.common.base.Splitter;

/**
 * Arquillian observer which controls Nodes lifecycle (on suite level and class level) and configuration of WildFly container
 * instances.
 *
 */
public class SunstoneObserver {
    private static final Logger LOGGER = SunstoneArquillianLogger.DEFAULT;

    @Inject
    private Instance<ContainerContext> containerContext;

    @Inject
    @ApplicationScoped
    private InstanceProducer<CloudsRegistry> cloudsRegistry;

    /**
     * Create a {@link CloudsRegistry} instance when Arquillian is started.
     */
    public void setupCloudRegistry(@Observes(precedence = 10) ManagerStarted event) {
        cloudsRegistry.set(new CloudsRegistry());
        // set system property "arquillian.xml" to an empty file
        // to avoid loading the descriptor in org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar
        try {
            Path path = Files.createTempFile("arquillian-", ".xml");
            File file = path.toFile();
            Files.write(path, "<arquillian></arquillian>".getBytes(StandardCharsets.UTF_8));
            file.deleteOnExit();
            System.setProperty("arquillian.xml", file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Unable to create custom arquillian.xml file.", e);
        }
    }

    /**
     * Creates class level nodes for test classes annotated with {@link WithNode}.
     */
    public void startNodesAndContainersForClass(@Observes StartClassContainers event, TestClass testClass,
            ServiceLoader serviceLoader, ContainerRegistry registry, CloudsRegistry cloudProviderRegistry) throws Exception {
        final List<CompletableFuture<Node>> futures = new ArrayList<>();
        final Set<String> nodeNames = new HashSet<>();

        WithNode[] withNodes = testClass.getJavaClass().getAnnotationsByType(WithNode.class);
        if (withNodes != null) {
            Arrays.stream(withNodes).forEach(wn -> nodeNames.add(wn.value()));
        }

        WithWildFlyContainer[] containers = testClass.getJavaClass().getAnnotationsByType(WithWildFlyContainer.class);
        if (containers != null) {
            Arrays.stream(containers).forEach(wn -> nodeNames.add(wn.value()));
        }
        for (String nodeName : nodeNames) {
            final String cloudProvider = getNodeProperties(nodeName).getProperty(ArquillianConfig.Node.PROVIDER);
            Objects.requireNonNull(cloudProvider, "Mandatory Cloud provider name (property '" + ArquillianConfig.Node.PROVIDER
                    + "') is not configured for node '" + nodeName + "'");
            futures.add(cloudProviderRegistry.ensureNodeAsync(cloudProvider, nodeName));
        }
        try {
            futures.forEach(f -> f.join());
            if (containers != null) {
                for (WithWildFlyContainer wflyContainer : containers) {
                    final String nodeName = wflyContainer.value();
                    LOGGER.debug("Registering class level node {} as container in Arquillian.", nodeName);
                    cloudProviderRegistry.configureWildflyContainer(nodeName, serviceLoader, registry, containerContext.get());
                }
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Starting Class level nodes failed. Nodes will be closed. Check if all resources was released successfully afterwards.",
                    e);
            for (CompletableFuture<Node> future : futures) {
                if (!future.isCompletedExceptionally()) {
                    try {
                        Node node = future.join();
                        node.close();
                    } catch (Exception ex) {
                        e.addSuppressed(ex);
                    }
                }
            }
            throw e;
        }
    }

    /**
     * Destroys test class level WildFly containers (and related Nodes) in test classes annotated with {@link WithWildFlyContainer}.
     */
    public void stopNodesAndContainersForClass(@Observes StopClassContainers event, TestClass testClass, ContainerRegistry registry,
            CloudsRegistry cloudProviderRegistry) throws Exception {
        final Set<String> nodeNames = new HashSet<>();

        WithNode[] withNodes = testClass.getJavaClass().getAnnotationsByType(WithNode.class);
        if (withNodes != null) {
            Arrays.stream(withNodes).forEach(wn -> nodeNames.add(wn.value()));
        }

        WithWildFlyContainer[] containers = testClass.getJavaClass().getAnnotationsByType(WithWildFlyContainer.class);
        if (containers != null) {
            for (WithWildFlyContainer wflyContainer : containers) {
                final String nodeName = wflyContainer.value();
                nodeNames.add(nodeName);
                LOGGER.debug("Removing WildFly container configuration for class level node '{}'", nodeName);
                cloudProviderRegistry.stopWildFlyContainerInRegistry(nodeName, registry, containerContext.get());
            }
        }
        cloudProviderRegistry.cleanupNodes(node->nodeNames.contains(node.getName()));
    }

    /**
     * Starts test suite level nodes listed in "arquillian.suite.start.nodes" and optionally registers them as WildFly
     * containers.
     */
    public void startNodesForSuite(@Observes BeforeSuite event, CloudsRegistry cloudsRegistry, ServiceLoader serviceLoader,
            ContainerRegistry registry, ContainerContext containerContext) throws Exception {
        LOGGER.debug("Starting Suite level nodes.");
        final List<CompletableFuture<Node>> futures = new ArrayList<>();
        try {
            final Set<String> names = new HashSet<>();
            processSuiteLeveNodes(nodeProperties -> {
                String provider = nodeProperties.getProperty(ArquillianConfig.Node.PROVIDER);
                cloudsRegistry.ensureProvider(provider);
                final String nodeName = nodeProperties.getName();
                if (!names.contains(nodeName)) {
                    futures.add(cloudsRegistry.ensureNodeAsync(provider, nodeName));
                    names.add(nodeName);
                }
            });
            futures.forEach(f -> f.join());
            // register Arquillian container if necessary
            processSuiteLeveNodes(nodeProperties -> {
                if (nodeProperties.getPropertyAsBoolean(ArquillianConfig.Node.CONTAINER_REGISTER, false)) {
                    try {
                        cloudsRegistry.configureWildflyContainer(nodeProperties.getName(), serviceLoader, registry,
                                containerContext);
                    } catch (Exception e) {
                        throw new RuntimeException("WildFly container registration failed", e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Starting Suite level nodes failed. Cloud providers will be closed. Check if all resources was released successfully afterwards.", e);
            for (CompletableFuture<Node> future : futures) {
                if (!future.isCompletedExceptionally()) {
                    try {
                        Node node = future.join();
                        node.close();
                    } catch (Exception ex) {
                        e.addSuppressed(ex);
                    }
                }
            }
            processSuiteLeveNodes(nodeProperties -> {
                String provider = nodeProperties.getProperty(ArquillianConfig.Node.PROVIDER);
                try {
                    cloudsRegistry.destroyProvider(provider);
                } catch (Exception ex) {
                    LOGGER.error("Unable to destroy provider '{}'", provider, ex);
                    e.addSuppressed(ex);
                }
            });
            throw e;
        }
    }

    /**
     * Removes nodes created on suite level and destroys provider listed in "arquillian.suite.destroy.providers" cloud property.
     */
    public void stopNodesForSuite(@Observes AfterSuite event, ContainerRegistry registry, CloudsRegistry cloudsRegistry)
            throws LifecycleException {
        try {
            final Set<String> nodeNameSet = new HashSet<>();
            processSuiteLeveNodes(nodeProperties -> {
                LOGGER.debug("WildFly container {}  will be destroyed", nodeProperties.getName());
                nodeNameSet.add(nodeProperties.getName());
                if (nodeProperties.getPropertyAsBoolean(ArquillianConfig.Node.CONTAINER_REGISTER, false)) {
                    try {
                        cloudsRegistry.stopWildFlyContainerInRegistry(nodeProperties.getName(), registry,
                                containerContext.get());
                    } catch (Exception e) {
                        throw new RuntimeException("Stopping WildFly container failed", e);
                    }
                }

            });
            cloudsRegistry.cleanupNodes(node -> nodeNameSet.contains(node.getName()));
        } finally {
            iterateSuiteCsvProperty(ArquillianConfig.Suite.DESTROY_PROVIDERS, cloudsRegistry::destroyProvider);
        }
    }

    /**
     * Returns object properties for Node with given name.
     *
     * @param node name of Node
     */
    private static ObjectProperties getNodeProperties(String node) {
        return new ObjectProperties(ObjectType.NODE, node);
    }

    private void processSuiteLeveNodes(Consumer<ObjectProperties> consumer) {
        iterateSuiteCsvProperty(ArquillianConfig.Suite.START_NODES,
                nodeName->consumer.accept(getNodeProperties(nodeName)));
    }

    private void iterateSuiteCsvProperty(String propertyName,  Consumer<String> consumer) {
        Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .split(new ObjectProperties(ArquillianObjectType.TESTSUITE,
                        System.getProperty(ArquillianConfig.SYSTEM_PROPERTY_ARQUILLIAN_SUITE)).getProperty(propertyName, ""))
                .forEach(consumer::accept);
    }
}
