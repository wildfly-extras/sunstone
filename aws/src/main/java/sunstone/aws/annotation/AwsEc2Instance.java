package sunstone.aws.annotation;


import sunstone.annotation.Deployment;
import sunstone.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify AWS EC2 instance (VM) for injection purposes.
 * <br>
 * Injectable: {@link Hostname} and
 * <br>
 * For more information about possible injection, see {@link AwsResourceIdentificationAnnotation}
 * <br>
 * Archive deploy operation (using {@link Deployment}) is supported under the name defined by {@link Deployment#name()}.
 * <br>
 */
// represented by AwsIdentifiableSunstoneResource#EC2_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AwsResourceIdentificationAnnotation
public @interface AwsEc2Instance {
    String nameTag();
    String region() default "";
}
