package sunstone.core;


import sunstone.annotation.SunstoneProperty;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Used by {@link SunstoneExtension} to get {@link CoreSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class CoreSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        // support only selected annotations
        Optional<SunstoneProperty> propertyIdentifications = AnnotationUtils.getAnnotation(field.getAnnotations(), SunstoneProperty.class);
        if (propertyIdentifications.isPresent()) {
            return Optional.of(new CoreSunstoneResourceInjector(propertyIdentifications.get(), field.getType()));
        } else {
            return Optional.empty();
        }
    }
}
