package sunstone.core.spi;

import sunstone.core.api.SunstoneResourceInjector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Service that returns {@link SunstoneResourceInjector}. Provider returns a service and promises it can inject
 * {@code annotation} into a field of {@code clazz}, otherwise {@code Optional.empty()} is expected.
 */
public interface SunstoneResourceInjectorProvider {
    Optional<SunstoneResourceInjector> create(Field field);
}
