package aws.core.identification;


import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used for automatic resolution without providing any further details.
 * <br>
 * Injectable: {@link Hostname} and {@link OnlineManagementClient}
 * <br>
 * For more information about possible injection, see {@link AwsInjectionAnnotation}
 */
// represented by AzureIdentifiableSunstoneResource#REGION
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AwsInjectionAnnotation
public @interface AwsRegion {
    public String value();
}
