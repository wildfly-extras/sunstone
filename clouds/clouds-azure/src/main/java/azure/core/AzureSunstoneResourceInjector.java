package azure.core;


import azure.core.AzureIdentifiableSunstoneResource.Identification;
import azure.core.identification.AzureInjectionAnnotation;
import azure.core.identification.AzureVirtualMachine;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.EapMode;
import sunstone.api.inject.Hostname;
import sunstone.core.AnnotationUtils;
import sunstone.core.CreaperUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import static azure.core.AzureIdentifiableSunstoneResource.VM_INSTANCE;
import static java.lang.String.format;


/**
 * Handles injecting object related to Azure cloud.
 *
 * Heavily uses {@link AzureIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Azure cloud resources, the class relies on {@link AzureIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in root extension store so that they are closed once the root store is closed (end of suite)
 */
public class AzureSunstoneResourceInjector implements SunstoneResourceInjector {
    static Hostname resolveHostnameDI(Identification identification, AzureSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case VM_INSTANCE:
                VirtualMachine vm = identification.get(store, VirtualMachine.class);
                return vm.getPrimaryPublicIPAddress()::ipAddress;
            case WEB_APP:
                WebApp app = identification.get(store, WebApp.class);
                return app::defaultHostname;
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }

    static OnlineManagementClient resolveOnlineManagementClientDI(Identification identification, AzureSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == VM_INSTANCE) {
                AzureVirtualMachine annotation = (AzureVirtualMachine) identification.identification;
                if (annotation.mode() == EapMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(resolveHostnameDI(identification, store).get(), annotation.standalone());
                } else {
                    throw new UnsupportedSunstoneOperationException("Only standalone mode is supported for injecting OnlineManagementClient.");
                }
            } else {
                throw new UnsupportedSunstoneOperationException("Only Azure VM instance is supported for injecting OnlineManagementClient.");
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
    }

    static boolean canInject (Field field) {
        return Arrays.stream(field.getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), AzureInjectionAnnotation.class))
                .filter(AzureIdentifiableSunstoneResource::isSupported)
                .anyMatch(a -> AzureIdentifiableSunstoneResource.getType(a).isTypeSupportedForInject(field.getType()));

    }

    @Override
    public Object getAndRegisterResource(Annotation annotation, Class<?> fieldType, ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);

        AzureIdentifiableSunstoneResource.Identification identification = new AzureIdentifiableSunstoneResource.Identification(annotation);
        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (Hostname.class.isAssignableFrom(fieldType)) {
            injected = resolveHostnameDI(identification, store);
            Objects.requireNonNull(injected, "Unable to determine hostname.");
        } else if (AzureResourceManager.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            injected = store.getAzureArmClientOrCreate();
            Objects.requireNonNull(injected, "Unable to determine Azure ARM client.");
        } else if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = resolveOnlineManagementClientDI(identification, store);
            Objects.requireNonNull(client, "Unable to determine management client.");
            store.addSuiteLevelClosable(client);
            injected = client;
        }
        return injected;
    }
}
