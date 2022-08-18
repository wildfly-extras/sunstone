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
 * Deploy Azure template
 * Deployed as a stack in {@link BeforeAllCallback} and stack is deleted in {@link  AfterAllCallback}
 *
 * Arrays of parameters are considered to be set of touples {key1, value1, [ke2, value2]*}
 * Not all types are supported https://docs.microsoft.com/en-us/azure/azure-resource-manager/templates/data-types
 * Since all parameters are string, the Sunstone looks into the template to determine the type and parse the value
 * accordingly.
 *
 * Supported types:
 * <ul>
 *     <li>string</li>
 *     <li>securestring</li>
 *     <li>int</li>
 *     <li>bool</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAzureArmTemplates.class)
@Inherited
public @interface WithAzureArmTemplate {
    String value() default "";
    String[] parameters() default {};
    TemplateType type() default TemplateType.RESOURCE;
}
