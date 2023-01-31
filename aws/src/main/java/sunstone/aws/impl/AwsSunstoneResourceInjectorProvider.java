package sunstone.aws.impl;


import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AwsSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AwsSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        // if field is annotated by @SunstoneResource or annotation annotated by @AwsInjectionAnnotation and field
        // type is supported for that kind of injection, then AwsSunstoneResourceInjector should be able to inject
        if (AwsSunstoneResourceInjector.canInject(field)) {
            return Optional.of(new AwsSunstoneResourceInjector());
        } else {
            return Optional.empty();
        }
    }
}
