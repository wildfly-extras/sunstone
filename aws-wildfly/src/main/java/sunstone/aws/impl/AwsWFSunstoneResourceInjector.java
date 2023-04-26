package sunstone.aws.impl;


import sunstone.annotation.WildFly;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.inject.Hostname;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.lang.annotation.Annotation;
import java.util.Objects;

import static java.lang.String.format;


/**
 * Handles injecting object related to Aws cloud.
 *
 * Heavily uses {@link AwsWFIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Aws cloud resources, the class relies on {@link AwsWFIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in extension store so that they are closed once the store is closed
 */
public class AwsWFSunstoneResourceInjector implements SunstoneResourceInjector {

    private AwsWFIdentifiableSunstoneResource.Identification identification;
    private WildFly wildFly;
    private Class<?> fieldType;

    public AwsWFSunstoneResourceInjector(AwsWFIdentifiableSunstoneResource.Identification identification, WildFly wildFly, Class<?> fieldType) {
        this.identification = identification;
        this.wildFly = wildFly;
        this.fieldType = fieldType;
    }

    @Override
    public Object getResource(ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);

        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = AwsWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(identification, wildFly, store);
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
