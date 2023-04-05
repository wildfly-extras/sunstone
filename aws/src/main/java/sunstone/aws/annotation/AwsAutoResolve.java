package sunstone.aws.annotation;


import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used for automatic resolution without providing any further details.
 * <br>
 * Injectable: {@link S3Client}, {@link Ec2Client}
 * <br>
 * For more information about possible injection, see {@link AwsResourceIdentificationAnnotation}
 */
// represented by AwsIdentifiableSunstoneResource#AUTO
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AwsResourceIdentificationAnnotation
public @interface AwsAutoResolve {
    String region() default "";
}
