package sunstone.azure.annotation;


import sunstone.annotation.Deployment;
import sunstone.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify Azure virtual machine for injection purposes.
 * <br>
 * Injectable: {@link Hostname}
 * <br>
 * For more information about possible injection, see {@link AzureResourceIdentificationAnnotation}
 * <br>
 * Archive deploy operation (using {@link sunstone.annotation.Deployment}) is supported. Always deployed as a <b>ROOT.war</b> ignoring {@link Deployment#name()}.
 * <br>
 * For more information about possible archive deploy operation, see {@link AzureArchiveDeploymentAnnotation}
 */
// represented by AzureIdentifiableSunstoneResource#WEB_APP
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AzureResourceIdentificationAnnotation
public @interface AzureWebApplication {
    String name();

    String group() default "";
}
