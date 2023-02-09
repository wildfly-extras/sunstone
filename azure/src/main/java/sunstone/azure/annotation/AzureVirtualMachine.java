package sunstone.azure.annotation;


import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.Deployment;
import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.StandaloneMode;
import sunstone.annotation.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify Azure virtual machine for injection purposes.
 * <br>
 * Injectable: {@link Hostname} and {@link OnlineManagementClient}
 * <br>
 * For more information about possible injection, see {@link AzureInjectionAnnotation}
 * <br>
 * Archive deploy operation (using {@link Deployment}) is supported under the name defined by {@link Deployment#name()}.
 * <br>
 * For more information about possible archive deploy operation, see {@link AzureArchiveDeploymentAnnotation}
 */
// represented by AzureIdentifiableSunstoneResource#VM_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AzureInjectionAnnotation
@AzureArchiveDeploymentAnnotation
public @interface AzureVirtualMachine {
    String name();
    String group() default "";
    OperatingMode mode() default OperatingMode.STANDALONE;
    StandaloneMode standalone()  default @StandaloneMode();
    DomainMode domain()  default @DomainMode();
}