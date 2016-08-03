package org.wildfly.extras.sunstone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class level (container) annotation which marks test classes for which should be configured WildFly containers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE })
public @interface WithWildFlyContainers {

    /**
     * Containers which is also used for container identification in Arquillian.
     */
    WithWildFlyContainer[] value();

}
