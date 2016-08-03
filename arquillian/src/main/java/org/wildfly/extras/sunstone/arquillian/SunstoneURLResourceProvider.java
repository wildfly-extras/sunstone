package org.wildfly.extras.sunstone.arquillian;

import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.Node;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Arquillian ResourceProvider implementation which workarounds wrong host part of URLs injected to tests. It extends
 * {@link URLResourceProvider}, and in the resulting URL replaces host with the value from instance.
 *
 */
public class SunstoneURLResourceProvider extends URLResourceProvider {
    private static final Logger LOGGER = SunstoneArquillianLogger.DEFAULT;

    @Inject
    private Instance<ContainerContext> containerContext;
    @Inject
    private Instance<CloudsRegistry> registry;

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL origUrl = (URL) super.doLookup(resource, qualifiers);
        URL result = origUrl;
        if (origUrl != null) {
            String containerId = containerContext.get().getActiveId();
            Set<Node> nodeSet = registry.get().getAllNodes();
            Node containerNode = null;
            if (containerId != null) {

                for (Node node : nodeSet) {
                    if (containerId.equals(node.getName())) {
                        containerNode = node;
                        break;
                    }
                }
            } else if (nodeSet.size() == 1) {
                containerNode = nodeSet.iterator().next();
            }
            if (containerNode != null) {
                try {
                    result = new URL(origUrl.getProtocol(), containerNode.getPublicAddress(), containerNode.getPublicTcpPort(origUrl.getPort()),
                            origUrl.getFile());
                    LOGGER.debug("URL was changed from {} to {}", origUrl, result);
                } catch (MalformedURLException e) {
                    LOGGER.error("Unable to update URL", e);
                }
            } else {
                LOGGER.warn("Unable to find correct node for fixing injected URL");
            }
        }

        return result;
    }

}