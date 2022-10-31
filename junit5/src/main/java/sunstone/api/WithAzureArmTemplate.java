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
 * Deploy Azure template
 * Deployed as into resource group defined in sunstone.properties.
 * Deployed in {@link BeforeAllCallback} and resource group is deleted and recreated in {@link  AfterAllCallback}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAzureArmTemplateRepetable.class)
@ExtendWith({SunstoneExtension.class})
@Inherited
public @interface WithAzureArmTemplate {
    String value();

    /**
     * Array of parameters: [key1, value1, key2, value2, ... ]
     *
     * Values may can be an expression - 'value-${variable:default}' - var is resolved from system properties.
     * <p>
     * Arrays of parameters are considered to be set of touples {key1, value1, [ke2, value2]*}
     * Not all types are supported https://docs.microsoft.com/en-us/azure/azure-resource-manager/templates/data-types
     * Since all parameters are string, the Sunstone looks into the template to determine the type and parse the value
     * accordingly.
     * <p>
     * Supported types:
     * <ul>
     *     <li>string</li>
     *     <li>securestring</li>
     *     <li>int</li>
     *     <li>bool</li>
     * </ul>
     */
    String[] parameters() default {};
}
