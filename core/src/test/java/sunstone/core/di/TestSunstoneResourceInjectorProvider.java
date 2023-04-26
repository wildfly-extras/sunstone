package sunstone.core.di;


import sunstone.annotation.SunstoneProperty;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.reflect.Field;
import java.util.Optional;

public class TestSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        if (AnnotationUtils.getAnnotation(field.getAnnotations(), SunstoneProperty.class).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new TestSunstoneResourceInjector());
    }
}
