package sunstone.core.di;


import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.reflect.Field;
import java.util.Optional;

public class TestSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        return Optional.of(new TestSunstoneResourceInjector());
    }
}
