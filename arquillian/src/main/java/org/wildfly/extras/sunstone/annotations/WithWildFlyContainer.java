package org.wildfly.extras.sunstone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class level annotation which references instructs Arquillian extension to create WildFly container configuration for given
 * {@link org.wildfly.extras.sunstone.api.Node}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithWildFlyContainers.class)
@Target(value = { ElementType.TYPE })
public @interface WithWildFlyContainer {

    /**
     * Name of {@link org.wildfly.extras.sunstone.api.Node} for which Arquillian container configuration should be created.
     */
    String value();
}
