package sunstone.api;

import sunstone.core.SunstoneExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
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
@Repeatable(WithAwsCfTemplates.class)
public @interface WithAwsCfTemplate {
    String value() default "";
    String[] parameters() default {};
    TemplateType type() default TemplateType.RESOURCE;
}
