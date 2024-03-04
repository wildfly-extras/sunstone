package sunstone.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;
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
        SunstoneProperty sunstoneProperty = SunstoneProperty.class.cast(identification);
        if (!StringUtils.isBlank(sunstoneProperty.value())) {
            return SunstoneConfigResolver.getValue(sunstoneProperty.value(), fieldType);
        }
        if (!StringUtils.isBlank(sunstoneProperty.expression())) {
            return SunstoneConfigResolver.resolveExpression(sunstoneProperty.expression(), fieldType);
        }
        throw new IllegalArgumentSunstoneException("Both value and expression in @SunstoneProperty are blank, which is not allowed.");
    }

    @Override
    public void closeResource(Object obj) throws Exception {
        // nothing to close
    }
}
