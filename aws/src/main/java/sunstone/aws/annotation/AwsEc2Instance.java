package sunstone.aws.annotation;


import sunstone.aws.impl.AwsConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify AWS EC2 instance (VM).
 * <br>
 * nameTag - required, "tag:Name" of the EC2 instance
 * <br>
 * region - Optional, region to look in. By default {@code sunstone.aws.region} Sunstone Config property is used
 */
// represented by AwsIdentifiableSunstoneResource#EC2_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AwsResourceIdentificationAnnotation
public @interface AwsEc2Instance {
    String nameTag();
    String region() default "${" + AwsConfig.REGION + "}";
}
