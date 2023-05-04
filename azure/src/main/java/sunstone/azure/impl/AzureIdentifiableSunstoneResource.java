package sunstone.azure.impl;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import sunstone.azure.annotation.AzureAutoResolve;
import sunstone.azure.annotation.AzurePgSqlServer;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.AzureWebApplication;
import sunstone.core.SunstoneConfig;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneCloudResourceException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;
import sunstone.inject.Hostname;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Used by {@link AzureSunstoneResourceInjector}
 *
 * Enum of Azure resources that can be identified by {@link AzureVirtualMachine} and so on and what field types are supported
 * for such identification by this module. Basically represent injection annotations and serves as a factory to get resources.
 *
 * I.e. for {@link #AUTO}, which effectively mean {@link AzureAutoResolve} is used, only {@link AzureResourceManager}
 * can be injected with such use of the annotation.
 *
 * Another example is  {@link AzureIdentifiableSunstoneResource#VM_INSTANCE},
 * which effectively mean {@link AzureVirtualMachine} is used. A user can inject {@link Hostname}
 */
enum AzureIdentifiableSunstoneResource {
    UNSUPPORTED(null),

    /**
     * Empty identification - e.g. {@link AzureAutoResolve}
     *
     * Injectable: {@link AzureResourceManager}
     *
     * Deployable: archive can not be deployed to such resource
     */
    AUTO (AzureAutoResolve.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {AzureResourceManager.class, PostgreSqlManager.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
    },

    /**
     * Azure Virtual machine instance identification, representation for {@link AzureVirtualMachine}
     *
     * Injectable: {@link Hostname}
     *
     * Deployable: archive can be deployed to such resource
     */
    VM_INSTANCE(AzureVirtualMachine.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {Hostname.class, VirtualMachine.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }

        @Override
        <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            if(!getRepresentedInjectionAnnotation().isAssignableFrom(injectionAnnotation.annotationType())) {
                throw new IllegalArgumentSunstoneException(format("Expected %s annotation type but got %s",
                        getRepresentedInjectionAnnotation().getName(), injectionAnnotation.annotationType().getName()));
            }
            AzureVirtualMachine vm = (AzureVirtualMachine) injectionAnnotation;
            String vmName = SunstoneConfig.resolveExpressionToString(vm.name());
            String vmGroup = SunstoneConfig.resolveExpressionToString(vm.group());
            Optional<VirtualMachine> azureVM = AzureUtils.findAzureVM(store.getAzureArmClientOrCreate(), vmName, vmGroup);
            return clazz.cast(azureVM.orElseThrow(() -> new SunstoneCloudResourceException(format("Unable to find '%s' Azure VM in '%s' resource group.", vmName, vmGroup))));
        }
    },

    /**
     * Azure Web application (application service) identification, representation for {@link AzureWebApplication}
     *
     * Injectable: {@link Hostname}
     *
     * Deployable: archive can be deployed to such resource
     */
    WEB_APP(AzureWebApplication.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {Hostname.class, WebApp.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            if(!getRepresentedInjectionAnnotation().isAssignableFrom(injectionAnnotation.annotationType())) {
                throw new IllegalArgumentSunstoneException(format("Expected %s annotation type but got %s",
                        getRepresentedInjectionAnnotation().getName(), injectionAnnotation.annotationType().getName()));
            }
            AzureWebApplication webApp = (AzureWebApplication) injectionAnnotation;
            String appName = SunstoneConfig.resolveExpressionToString(webApp.name());
            String appGroup = SunstoneConfig.resolveExpressionToString(webApp.group());
            Optional<WebApp> azureWebApp = AzureUtils.findAzureWebApp(store.getAzureArmClientOrCreate(), appName, appGroup);
            return clazz.cast(azureWebApp.orElseThrow(() -> new SunstoneCloudResourceException(format("Unable to find '%s' Azure Web App in '%s' resource group.", appName, appGroup))));
        }
    },

    /**
     * Azure Web application (application service) identification, representation for {@link AzureWebApplication}
     *
     * Injectable: {@link Hostname}, {@link Server}
     */
    PGSQL_SERVER(AzurePgSqlServer.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {Hostname.class, Server.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            if(!getRepresentedInjectionAnnotation().isAssignableFrom(injectionAnnotation.annotationType())) {
                throw new IllegalArgumentSunstoneException(format("Expected %s annotation type but got %s",
                        getRepresentedInjectionAnnotation().getName(), injectionAnnotation.annotationType().getName()));
            }
            AzurePgSqlServer pgsqlServer = (AzurePgSqlServer) injectionAnnotation;
            String serverName = SunstoneConfig.resolveExpressionToString(pgsqlServer.name());
            String serverGroup = SunstoneConfig.resolveExpressionToString(pgsqlServer.group());
            Optional<Server> azureWebApp = AzureUtils.findAzurePgSqlServer(store.getAzurePgSqlManagerOrCreate(), serverName, serverGroup);
            return clazz.cast(azureWebApp.orElseThrow(() -> new SunstoneCloudResourceException(format("Unable to find '%s' Azure PostgreSql Server in '%s' resource group.", serverName, serverGroup))));
        }
    };

    private final Class<?> representedInjectionAnnotation;

    Class<?> getRepresentedInjectionAnnotation() {
        return representedInjectionAnnotation;
    }

    AzureIdentifiableSunstoneResource(Class<?> representedInjectionAnnotation) {
        this.representedInjectionAnnotation = representedInjectionAnnotation;
    }

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
    <T> T get(Annotation injectionAnnotation, AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
        throw new UnsupportedSunstoneOperationException(format("%s annotation is nto supported for the type %s",
               injectionAnnotation.annotationType().getName(), this.toString()));
    }

    public static boolean isSupported(Annotation annotation) {
        return getType(annotation) != UNSUPPORTED;
    }
    public static AzureIdentifiableSunstoneResource getType(Annotation annotation) {
        if(AzureVirtualMachine.class.isAssignableFrom(annotation.annotationType())) {
            return VM_INSTANCE;
        } else if(AzureWebApplication.class.isAssignableFrom(annotation.annotationType())) {
            return WEB_APP;
        } else if(AzurePgSqlServer.class.isAssignableFrom(annotation.annotationType())) {
            return PGSQL_SERVER;
        } else if(AzureAutoResolve.class.isAssignableFrom(annotation.annotationType())) {
            return AUTO;
        } else {
            return UNSUPPORTED;
        }
    }

    /**
     * Serves as a wrapper over annotation providing {@link AzureIdentifiableSunstoneResource} type and shortcut to the
     * {@link AzureIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)} factory method.
     */
    static class Identification {
        final Annotation identification;
        final AzureIdentifiableSunstoneResource type;
        Identification(Annotation annotation) {
            this.type = AzureIdentifiableSunstoneResource.getType(annotation);
            this.identification = annotation;
        }
        <T> T get(AzureSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return type.get(identification, store, clazz);
        }
    }

}
