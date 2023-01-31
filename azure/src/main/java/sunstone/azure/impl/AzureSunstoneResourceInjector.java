package sunstone.azure.impl;


import sunstone.azure.api.AzureInjectionAnnotation;
import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.inject.Hostname;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.SunstoneException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;


/**
 * Handles injecting object related to Azure cloud.
 *
 * Heavily uses {@link AzureIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Azure cloud resources, the class relies on {@link AzureIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in the extension store so that they are closed once the store is closed
 */
public class AzureSunstoneResourceInjector implements SunstoneResourceInjector {

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
            injected = AzureIdentifiableSunstoneResourceUtils.resolveHostname(identification, store);
            Objects.requireNonNull(injected, "Unable to determine hostname.");
        } else if (AzureResourceManager.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            injected = store.getAzureArmClientOrCreate();
            Objects.requireNonNull(injected, "Unable to determine Azure ARM client.");
        } else if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = AzureIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(identification, store);
            Objects.requireNonNull(client, "Unable to determine management client.");
            store.addClosable(client);
            injected = client;
        }
        return injected;
    }
}
