package org.wildfly.extras.sunstone.arquillian;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.annotations.InjectCloudProvider;
import org.wildfly.extras.sunstone.api.CloudProvider;

/**
 * Arquillian ResourceProvider for {@link CloudProvider} instances. It should be used together with {@link InjectCloudProvider}
 * annotation. CloudProvider is only injected if it already exists in the {@link CloudsRegistry}.
 *
 */
public class CloudProviderResourceProvider implements ResourceProvider {

    private static final Logger LOGGER = SunstoneArquillianLogger.DEFAULT;

    @Inject
    private Instance<CloudsRegistry> registry;

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        final CloudsRegistry cloudsRegistry = registry.get();
        for (Annotation an : qualifiers) {
            if (an.annotationType() == InjectCloudProvider.class) {
                return cloudsRegistry.getProvider(((InjectCloudProvider) an).value());
            }
        }
        // check if only one cloud provider is in the registry - and inject it
        final Set<String> cloudProviderNames = cloudsRegistry.getCloudProviderNames();
        if (cloudProviderNames.size() == 1) {
            final String singleProvider = cloudProviderNames.iterator().next();
            LOGGER.warn(
                    "The @InjectCloudProvider annotation was not used to select CloudProvider to inject into the test. The CloudRegistry holds only one entry ('{}') which will be used instead.",
                    singleProvider);
            return cloudsRegistry.getProvider(singleProvider);
        }
        throw new RuntimeException(
                "Unable to find CloudProvider to inject. Use annotation @" + InjectCloudProvider.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(java.lang.Class)
     */
    public boolean canProvide(Class<?> type) {
        return CloudProvider.class == type;
    }

}