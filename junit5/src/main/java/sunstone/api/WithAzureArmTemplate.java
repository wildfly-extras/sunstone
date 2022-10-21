package sunstone.api;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;

/**
 * Deploy Azure template
 * Deployed as into resource group defined in sunstone.properties.
 * Deployed in {@link BeforeAllCallback} and resource group is deleted and recreated in {@link  AfterAllCallback}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAzureArmTemplateRepetable.class)
@Inherited
public @interface WithAzureArmTemplate {
    String value();

    /**
     * Array of parameters: [key1, value1, key2, value2, ... ]
     * Overrides {@link #parametersProvider()}
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
    String[] parameters() default {};

    /**
     * {@link #parameters()} can provide only constants. Use this if you need to provide parameters dynamically, i.e.
     * string+random_string. Provided parameters are overriden by {@link #parameters()}.
     *
     * Values are treated same way as for {@link #parameters()}
     */
    Class<?  extends AbstractParameterProvider> parametersProvider() default AbstractParameterProvider.DEFAULT.class;
    ValueType type() default ValueType.RESOURCE;

    boolean testSuiteLevel() default false;
}
