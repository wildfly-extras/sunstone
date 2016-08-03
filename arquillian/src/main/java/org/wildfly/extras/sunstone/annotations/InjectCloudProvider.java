package org.wildfly.extras.sunstone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field level annotation which marks fields for injecting named  ({@link org.wildfly.extras.sunstone.api.CloudProvider}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.PARAMETER })
public @interface InjectCloudProvider {

    /**
     * Name of ({@link org.wildfly.extras.sunstone.api.CloudProvider}) to inject.
     */
    String value();

}
