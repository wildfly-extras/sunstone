package org.wildfly.extras.sunstone.arquillian;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.annotations.InjectNode;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

/**
 * Arquillian ResourceProvider for {@link Node} instances. It should be used together with {@link InjectNode} annotation. The
 * Node is only injected if it already exists in the {@link CloudsRegistry}.
 *
 */
public class NodeResourceProvider implements ResourceProvider {

    private static final Logger LOGGER = SunstoneArquillianLogger.DEFAULT;

    @Inject
    private Instance<CloudsRegistry> registry;

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        final CloudsRegistry cloudsRegistry = registry.get();
        for (Annotation an : qualifiers) {
            if (an.annotationType() == InjectNode.class) {
                final InjectNode injectNode = (InjectNode) an;
                final String node = injectNode.value();
                String provider = new ObjectProperties(ObjectType.NODE, node).getProperty(ArquillianConfig.Node.PROVIDER);
                return cloudsRegistry.getNode(provider, node);
            }
        }
        // check if only one node is in the registry - and inject it
        Set<Node> nodeSet = cloudsRegistry.getAllNodes();
        if (nodeSet.size() == 1) {
            final Node singleNode = nodeSet.iterator().next();
            LOGGER.warn(
                    "The @InjectNode annotation was not used to select Node to inject into the test. A Node registry holds only one entry ('{}') which will be used instead.",
                    singleNode.getName());
            return singleNode;
        }
        throw new RuntimeException("Unable to find Node to inject. Use annotation @" + InjectNode.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(java.lang.Class)
     */
    public boolean canProvide(Class<?> type) {
        return Node.class.equals(type);
    }

}