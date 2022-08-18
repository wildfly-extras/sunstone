package sunstone.api;

import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension that enable Sunstone to deploy, inject and manage Cloud resources.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@ExtendWith({SunstoneExtension.class})
@Inherited
public @interface Sunstone {
}
