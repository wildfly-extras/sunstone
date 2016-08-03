package org.wildfly.extras.sunstone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field level annotation which marks fields for injecting Node instances with name configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.PARAMETER })
public @interface InjectNode {

    /**
     * Named Node configuration
     */
    String value();

}
