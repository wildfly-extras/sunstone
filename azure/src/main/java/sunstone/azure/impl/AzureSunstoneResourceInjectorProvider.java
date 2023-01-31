package sunstone.azure.impl;


import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AzureSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AzureSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        // if field is annotated by @SunstoneResource or annotation annotated by @AzureInjectionAnnotation and field
        // type is supported for that kind of injection, then AzureSunstoneResourceInjector should be able to inject
        if (AzureSunstoneResourceInjector.canInject(field)) {
            return Optional.of(new AzureSunstoneResourceInjector());
        } else {
            return Optional.empty();
        }
    }
}
