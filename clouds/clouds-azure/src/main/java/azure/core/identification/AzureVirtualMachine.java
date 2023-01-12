package azure.core.identification;


import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.DomainMode;
import sunstone.api.EapMode;
import sunstone.api.StandaloneMode;
import sunstone.api.inject.Hostname;

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
 */
// represented by AzureIdentifiableSunstoneResource#VM_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AzureInjectionAnnotation
public @interface AzureVirtualMachine {
    String name();
    String group() default "";
    EapMode mode() default EapMode.STANDALONE;
    StandaloneMode standalone()  default @StandaloneMode();
    DomainMode domain()  default @DomainMode();
}
