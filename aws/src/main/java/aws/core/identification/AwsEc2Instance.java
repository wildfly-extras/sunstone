package aws.core.identification;


import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.Deployment;
import sunstone.api.DomainMode;
import sunstone.api.EapMode;
import sunstone.api.StandaloneMode;
import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify AWS EC2 instance (VM) for injection purposes.
 * <br>
 * Injectable: {@link Hostname} and {@link OnlineManagementClient}
 * <br>
 * For more information about possible injection, see {@link AwsInjectionAnnotation}
 * <br>
 * Archive deploy operation (using {@link Deployment}) is supported under the name defined by {@link Deployment#name()}.
 * <br>
 * For more information about possible archive deploy operation, see {@link AwsArchiveDeploymentAnnotation}
 */
// represented by AwsIdentifiableSunstoneResource#EC2_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AwsInjectionAnnotation
@AwsArchiveDeploymentAnnotation
public @interface AwsEc2Instance {
    String nameTag();
    String region() default "";
    EapMode mode() default EapMode.STANDALONE;
    StandaloneMode standalone()  default @StandaloneMode();
    DomainMode domain()  default @DomainMode();
}
