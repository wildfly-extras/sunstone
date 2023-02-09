package sunstone.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define {@link AbstractSetupTask} that configure environment right after Cloud resources are deployed and before.
 *
 * The class may inject static/non-static resources using annotations annotated by {@link SunstoneInjectionAnnotation}
 * that are brought bu modules like sunstone-clouds-aws or sunstone-clouds-azure.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@ExtendWith(SunstoneExtension.class)
@Inherited
public @interface Setup {
    Class<? extends AbstractSetupTask> [] value();
}
