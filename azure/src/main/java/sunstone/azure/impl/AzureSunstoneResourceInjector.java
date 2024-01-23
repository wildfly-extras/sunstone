package sunstone.azure.impl;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.inject.Hostname;
import sunstone.azure.annotation.AzureResourceIdentificationAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
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

    private AzureIdentifiableSunstoneResource.Identification identification;
    private Class<?> fieldType;
    public AzureSunstoneResourceInjector(AzureIdentifiableSunstoneResource.Identification identification, Class<?> fieldType) {
        this.identification = identification;
        this.fieldType = fieldType;
    }

    static boolean canInject (Field field) {
        return Arrays.stream(field.getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), AzureResourceIdentificationAnnotation.class))
                .filter(AzureIdentifiableSunstoneResource::isSupported)
                .anyMatch(a -> AzureIdentifiableSunstoneResource.getType(a).isTypeSupportedForInject(field.getType()));

    }

    @Override
    public Object getResource(ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);

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
        } else if (PostgreSqlManager.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            injected = store.getAzurePgSqlManagerOrCreate();
            Objects.requireNonNull(injected, "Unable to determine Azure ARM client.");
        } else {
            // VirtualMachine, WebApp, AppServicePlan, Server
            injected = identification.get(store);
            Objects.requireNonNull(injected, "Unable to get abstraction object for "+ identification.identification);

        }
        return injected;
    }

    @Override
    public void closeResource(Object obj) throws Exception {
        if (Hostname.class.isAssignableFrom(obj.getClass())
                || AzureResourceManager.class.isAssignableFrom(obj.getClass())
                || WebApp.class.isAssignableFrom(obj.getClass())
                || AppServicePlan.class.isAssignableFrom(obj.getClass())
                || Server.class.isAssignableFrom(obj.getClass())
                || PostgreSqlManager.class.isAssignableFrom(obj.getClass())
                || VirtualMachine.class.isAssignableFrom(obj.getClass())) {
            // nothing to close
        } else {
            throw new IllegalArgumentSunstoneException("Unknown type " + obj.getClass());
        }

    }
}
