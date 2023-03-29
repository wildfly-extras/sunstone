package sunstone.azure.impl;


import sunstone.annotation.WildFly;
import sunstone.azure.annotation.AzureResourceIdentificationAnnotation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.inject.Hostname;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;


/**
 * Handles injecting object related to Azure cloud.
 *
 * Heavily uses {@link AzureWFIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Azure cloud resources, the class relies on {@link AzureWFIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in the extension store so that they are closed once the store is closed
 */
public class AzureWFSunstoneResourceInjector implements SunstoneResourceInjector {

    private AzureWFIdentifiableSunstoneResource.Identification identification;
    private WildFly wildFly;
    private Class<?> fieldType;
    public AzureWFSunstoneResourceInjector(AzureWFIdentifiableSunstoneResource.Identification identification, WildFly wildFly, Class<?> fieldType) {
        this.identification = identification;
        this.wildFly = wildFly;
        this.fieldType = fieldType;
    }

    static boolean canInject (Annotation[] fieldAnnotations, Class<?> fieldType) {
        return Arrays.stream(fieldAnnotations)
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), AzureResourceIdentificationAnnotation.class))
                .filter(AzureWFIdentifiableSunstoneResource::isSupported)
                .anyMatch(a -> AzureWFIdentifiableSunstoneResource.getType(a).isTypeSupportedForInject(fieldType));

    }

    @Override
    public Object getResource(ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);

        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = AzureWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(identification, wildFly, store);
            Objects.requireNonNull(client, "Unable to determine management client.");
            injected = client;
        }
        return injected;
    }

    @Override
    public void closeResource(Object obj) throws Exception {
        if (OnlineManagementClient.class.isAssignableFrom(obj.getClass())) {
            ((OnlineManagementClient) obj).close();
        } else {
            throw new IllegalArgumentSunstoneException("Unknown type " + obj.getClass());
        }
    }
}
