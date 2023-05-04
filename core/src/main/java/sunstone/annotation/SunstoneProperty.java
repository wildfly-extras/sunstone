package sunstone.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@CloudResourceIdentificationAnnotation
public @interface SunstoneProperty {
    /**
     * Property name that should be resolved. Property can be defined in sunstone.properties, system prperties, ...
     * <br>
     * This has higher priority than expression parameter. If both are set, expression won't be considered.
     */
    String value() default "";

    /**
     * If you need to resolve complicated expression, set this parameter, e.g. "${property-name}-${suffix}"
     * <br>
     * {@code @SunstoneProperty("propertyName")} and {@code @SunstoneProperty(expression="${propertyName}")} injects same string.
     */
    String expression() default "";
}
