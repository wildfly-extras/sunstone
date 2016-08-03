package org.wildfly.extras.sunstone.arquillian;

import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * Arquillian Extension which registers clouds related services and observers.
 *
 */
public class SunstoneExtension implements LoadableExtension {

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.core.spi.LoadableExtension#register(org.jboss.
     * arquillian.core.spi.LoadableExtension.ExtensionBuilder)
     */
    public void register(ExtensionBuilder builder) {
        // check if the extension is not disabled by a system property
        if (System.getProperty(ArquillianConfig.SYSTEM_PROPERTY_DISABLE_EXTENSION) != null) {
            return;
        }
        builder.observer(SunstoneObserver.class).service(ResourceProvider.class, CloudsRegistryResourceProvider.class)
                .service(ResourceProvider.class, CloudProviderResourceProvider.class)
                .service(ResourceProvider.class, NodeResourceProvider.class)
                .override(ResourceProvider.class, URLResourceProvider.class, SunstoneURLResourceProvider.class);
    }

}
