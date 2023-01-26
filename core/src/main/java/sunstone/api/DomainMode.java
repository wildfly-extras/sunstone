package sunstone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_PARAMETER)
public @interface DomainMode {
    String host() default "";
    String profile() default "";
    String user() default "";
    String password() default "";
    String port() default "";
}
