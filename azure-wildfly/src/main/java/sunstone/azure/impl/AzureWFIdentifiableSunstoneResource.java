package sunstone.azure.impl;


import sunstone.azure.annotation.AzureAutoResolve;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.AzureWebApplication;
import com.azure.resourcemanager.AzureResourceManager;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.inject.Hostname;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Used by {@link AzureWFSunstoneResourceInjector}
 *
 * Enum of Azure resources that can be identified by {@link AzureVirtualMachine} and so on and what field types are supported
 * for such identification by this module. Basically represent injection annotations and serves as a factory to get resources.
 *
 * I.e. for {@link #AUTO}, which effectively mean {@link AzureAutoResolve} is used, only {@link AzureResourceManager}
 * can be injected with such use of the annotation.
 *
 * Another example is  {@link AzureWFIdentifiableSunstoneResource#VM_INSTANCE},
 * which effectively mean {@link AzureVirtualMachine} is used. A user can inject {@link Hostname} and
 * {@link OnlineManagementClient}.
 */
enum AzureWFIdentifiableSunstoneResource {
    UNSUPPORTED(null),

    /**
     * Azure Virtual machine instance identification, representation for {@link AzureVirtualMachine}
     *
     * Injectable: {@link Hostname} and {@link OnlineManagementClient}
     *
     * Deployable: archive can be deployed to such resource
     */
    VM_INSTANCE(AzureVirtualMachine.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {OnlineManagementClient.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        boolean deployToWildFlySupported() {
            return true;
        }

        @Override
        <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return AzureIdentifiableSunstoneResource.VM_INSTANCE.get(injectionAnnotation, store, clazz);
        }
    },

    /**
     * Azure Web application (application service) identification, representation for {@link AzureWebApplication}
     *
     * Injectable: none (azure-wildfly module does not extend azure for this resource)
     *
     * Deployable: archive can be deployed to such resource
     */
    WEB_APP(AzureWebApplication.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        boolean deployToWildFlySupported() {
            return true;
        }
        @Override
        <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return AzureIdentifiableSunstoneResource.WEB_APP.get(injectionAnnotation, store, clazz);
        }
    };

    private final Class<?> representedInjectionAnnotation;

    Class<?> getRepresentedInjectionAnnotation() {
        return representedInjectionAnnotation;
    }

    AzureWFIdentifiableSunstoneResource(Class<?> representedInjectionAnnotation) {
        this.representedInjectionAnnotation = representedInjectionAnnotation;
    }

    @Override
    public String toString() {
        if (representedInjectionAnnotation == null) {
            return "unsupported AzureIdentifiableSunstoneResource type";
        } else {
            return format("%s representing %s injection annotation", this.name(), representedInjectionAnnotation.getName());
        }
    }

    boolean isTypeSupportedForInject(Class<?> type) {
        return false;
    }
    boolean deployToWildFlySupported() {
        return false;
    }
    <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
        throw new UnsupportedSunstoneOperationException(format("%s annotation is nto supported for the type %s",
               injectionAnnotation.annotationType().getName(), this.toString()));
    }

    public static boolean isSupported(Annotation annotation) {
        return getType(annotation) != UNSUPPORTED;
    }
    public static AzureWFIdentifiableSunstoneResource getType(Annotation annotation) {
        if(AzureVirtualMachine.class.isAssignableFrom(annotation.annotationType())) {
            return VM_INSTANCE;
        } else if(AzureWebApplication.class.isAssignableFrom(annotation.annotationType())) {
            return WEB_APP;
        } else {
            return UNSUPPORTED;
        }
    }

    /**
     * Serves as a wrapper over annotation providing {@link AzureWFIdentifiableSunstoneResource} type and shortcut to the
     * {@link AzureWFIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)} factory method.
     */
    static class Identification {
        final Annotation identification;
        final AzureWFIdentifiableSunstoneResource type;
        Identification(Annotation annotation) {
            this.type = AzureWFIdentifiableSunstoneResource.getType(annotation);
            this.identification = annotation;
        }
        <T> T get(AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return type.get(identification, store, clazz);
        }
    }

}
