package aws.api;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.api.Parameter;
import sunstone.api.SunstoneCloudDeployAnnotation;
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
 * Deployed as a stack in {@link BeforeAllCallback} and whole stack is deleted in {@link  AfterAllCallback} or
 * once the suite is finished (see {@link WithAwsCfTemplate#perSuite()})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(WithAwsCfTemplateRepeatable.class)
@ExtendWith({SunstoneExtension.class})
@SunstoneCloudDeployAnnotation
@Inherited
public @interface WithAwsCfTemplate {
    /**
     * Template file located in resources
     */
    String template();

    /**
     * Region that should be used for creating resource group. Expression is allowed, e.g. {@code abc-${var:default}-xyz} -
     * var is resolved from system properties.
     *
     * If empty, {@code sunstone.aws.region} from {@code sunstone.properties} is used.
     *
     * For the list of available regions see {@link software.amazon.awssdk.regions.Region}
     */
    String region() default "";

    /**
     * Array of parameters
     * Values may can be an expression - 'value-${variable:default}' - var is resolved from system properties.
    */
    Parameter[] parameters() default {};

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
