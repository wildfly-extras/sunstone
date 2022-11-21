package sunstone.api;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deploy CloudFormation template
 * <p>
 * Deployed as a stack in {@link BeforeAllCallback} and stack is deleted in {@link  AfterAllCallback}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAwsCfTemplateRepetable.class)
@ExtendWith({SunstoneExtension.class})
@Inherited
public @interface WithAwsCfTemplate {
    /**
     * Template file located in resources.
     */
    String template();

    /**
     * Array of parameters
     * Values may can be an expression - 'value-${variable:default}' - var is resolved from system properties.
    */
    Parameter[] parameters() default {};
}
