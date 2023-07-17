package sunstone.aws.annotation;


import sunstone.aws.impl.AwsConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify Aws RDS.
 * <br>
 * name - required, name of the server
 * <br>
 * region - optional, region where the server is located, default is {@link AwsConfig#REGION}
 */
// represented by AwsIdentifiableSunstoneResource#RDS_SERVICE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AwsResourceIdentificationAnnotation
public @interface AwsRds {
    String name();
    String region() default "${" + AwsConfig.REGION + "}";
}
