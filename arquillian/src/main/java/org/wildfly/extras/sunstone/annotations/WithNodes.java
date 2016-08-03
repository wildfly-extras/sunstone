package org.wildfly.extras.sunstone.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for {@link WithNode}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE })
public @interface WithNodes {

    /**
     * Containers which is also used for container identification in Arquillian.
     */
    WithNode[] value();

}
