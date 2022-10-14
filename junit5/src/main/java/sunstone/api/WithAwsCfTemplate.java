package sunstone.api;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;

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
@Inherited
public @interface WithAwsCfTemplate {
    String value();

    String[] parameters() default {};

    ValueType type() default ValueType.RESOURCE;
}
