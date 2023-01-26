package sunstone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deploy CloudFormation template
 *
 * Deployed as a stack in BeforeAll and stack is deleted in AfterAll
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface WithAwsCfTemplateRepeatable {
    WithAwsCfTemplate[] value();
}
