package sunstone.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.annotation.SunstoneProperty;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.lang.annotation.Annotation;

import static java.lang.String.format;


public class CoreSunstoneResourceInjector implements SunstoneResourceInjector {

    private Annotation identification;
    private Class<?> fieldType;
    public CoreSunstoneResourceInjector(Annotation annotation, Class<?> fieldType) {
        this.identification = annotation;
        this.fieldType = fieldType;
    }

    @Override
    public Object getResource(ExtensionContext ctx) throws SunstoneException {
        Object injected = null;

        if(!SunstoneProperty.class.isAssignableFrom(identification.annotationType())) {
            throw new IllegalArgumentSunstoneException(format("Only %s is supported for injection.", SunstoneProperty.class));
        }
        if (!String.class.isAssignableFrom(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s", SunstoneProperty.class, fieldType));
        }
        SunstoneProperty sunstoneProperty = SunstoneProperty.class.cast(identification);
        return SunstoneConfig.getValue(sunstoneProperty.value(), fieldType);
    }

    @Override
    public void closeResource(Object obj) throws Exception {
        // nothing to close
    }
}
