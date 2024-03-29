package sunstone.azure.annotation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneCloudDeployAnnotation;
import sunstone.azure.impl.AzureConfig;
import sunstone.core.SunstoneExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deploy Azure template
 * Deployed in {@link BeforeAllCallback} and whole resource group is deleted and recreated in {@link  AfterAllCallback}
 * once the suite is finished (see {@link WithAzureArmTemplate#perSuite()})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAzureArmTemplateRepeatable.class)
@ExtendWith({SunstoneExtension.class})
@SunstoneCloudDeployAnnotation
@Inherited
public @interface WithAzureArmTemplate {
    String template();

    /**
     * Array of parameters.
     *
     * Values may can be an expression - {@code abc-${variable:default}-xyz}.
     * <p>
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
    Parameter[] parameters() default {};

    /**
     * Resource group that should be used for deployment. Expression is allowed, e.g. {@code abc-${var:default}-xyz}.
     * Resources in a group share lifecycle and the group is deleted as it is for undeploy operation.
     *
     * By default {@code sunstone.azure.group} Sunstone Config property is used.
     */
    String group() default "${" + AzureConfig.GROUP + "}";

    /**
     * Region that should be used for creating resource group. Expression is allowed, e.g. {@code abc-${var:default}-xyz}.
     *
     * By default {@code sunstone.azure.region} Sunstone Config property is used.
     *
     * For the list of available regions see {@link com.azure.core.management.Region}
     */
    String region() default "${" + AzureConfig.REGION + "}";

    /**
     * <p>
     * True if resources supposed to be managed at suite level.
     * <p>
     * The template is deployed only once and undeployed once the suite is done.
     * <p>
     * Suite is a set of test classes that runs in a bulk not interfering with other suites (sets of test classes).
     * Suite may be defined as a surefire run or as {@link org.junit.platform.suite.api.Suite}
     */
    boolean perSuite() default false;
}
