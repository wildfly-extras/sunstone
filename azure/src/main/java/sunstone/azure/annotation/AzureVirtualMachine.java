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
 * Archive deploy operation (using {@link Deployment}) is supported under the name defined by {@link Deployment#name()}.
 */
// represented by AzureIdentifiableSunstoneResource#VM_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AzureResourceIdentificationAnnotation
public @interface AzureVirtualMachine {
    String name();
    String group() default "";
}
