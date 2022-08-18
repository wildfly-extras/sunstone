package sunstone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define {@link AbstractSetupTask} that configure environment right after Cloud resources are deployed and before.
 *
 * The class may inject static/non-static resources using {@link SunstoneResource} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface Setup {
    Class<? extends AbstractSetupTask> [] value();
}
