package azure.core.identification;


import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify Azure virtual machine for injection purposes.
 * <br>
 * Injectable: {@link Hostname}
 * <br>
 * For more information about possible injection, see {@link AzureInjectionAnnotation}
 */
// represented by AzureIdentifiableSunstoneResource#WEB_APP
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AzureInjectionAnnotation
public @interface AzureWebApplication {
    String name();

    String group() default "";
}
