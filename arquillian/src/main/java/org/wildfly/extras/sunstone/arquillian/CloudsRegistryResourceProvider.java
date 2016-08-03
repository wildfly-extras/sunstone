package org.wildfly.extras.sunstone.arquillian;

import java.lang.annotation.Annotation;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * Arquillian ResourceProvider for {@link CloudsRegistry} instances.
 *
 */
public class CloudsRegistryResourceProvider implements ResourceProvider {

    @Inject
    private Instance<CloudsRegistry> registry;

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return registry.get();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(java.lang.Class)
     */
    public boolean canProvide(Class<?> type) {
        return CloudsRegistry.class.isAssignableFrom(type);
    }

}