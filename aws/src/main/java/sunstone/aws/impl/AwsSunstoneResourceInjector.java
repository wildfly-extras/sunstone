package sunstone.aws.impl;


import sunstone.aws.annotation.AwsAutoResolve;
import sunstone.aws.annotation.AwsInjectionAnnotation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.core.SunstoneConfig;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.annotation.inject.Hostname;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;


/**
 * Handles injecting object related to Aws cloud.
 *
 * Heavily uses {@link AwsIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Aws cloud resources, the class relies on {@link AwsIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in extension store so that they are closed once the store is closed
 */
public class AwsSunstoneResourceInjector implements SunstoneResourceInjector {
    static Ec2Client resolveEc2ClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        Ec2Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.AUTO) {
            AwsAutoResolve annotation = (AwsAutoResolve) identification.identification;
            client = AwsUtils.getEC2Client(annotation.region().isBlank() ? SunstoneConfig.getString(AwsConfig.REGION) : SunstoneConfig.resolveExpressionToString(annotation.region()));
        } else {
            throw new UnsupportedSunstoneOperationException("EC2 Client may be injected only with " + AwsIdentifiableSunstoneResource.AUTO);
        }
        return client;
    }

    static S3Client resolveS3ClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        S3Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.AUTO) {
            AwsAutoResolve annotation = (AwsAutoResolve) identification.identification;
            client = AwsUtils.getS3Client(annotation.region().isBlank() ? SunstoneConfig.getString(AwsConfig.REGION) : SunstoneConfig.resolveExpressionToString(annotation.region()));
        } else {
            throw new UnsupportedSunstoneOperationException("EC2 Client may be injected only with " + AwsIdentifiableSunstoneResource.AUTO);
        }
        return client;
    }

    static boolean canInject (Field field) {
        return Arrays.stream(field.getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), AwsInjectionAnnotation.class))
                .filter(AwsIdentifiableSunstoneResource::isSupported)
                .anyMatch(a -> AwsIdentifiableSunstoneResource.getType(a).isTypeSupportedForInject(field.getType()));

    }

    @Override
    public Object getAndRegisterResource(Annotation annotation, Class<?> fieldType, ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);

        AwsIdentifiableSunstoneResource.Identification identification = new AwsIdentifiableSunstoneResource.Identification(annotation);
        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (Hostname.class.isAssignableFrom(fieldType)) {
            injected = AwsIdentifiableSunstoneResourceUtils.resolveHostname(identification, store);
            Objects.requireNonNull(injected, "Unable to determine hostname.");
        } else if (Ec2Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            Ec2Client client = resolveEc2ClientDI(identification, store);
            store.addClosable(client);
            injected = client;
            Objects.requireNonNull(injected, "Unable to determine AWS EC2 client.");
        } else if (S3Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            S3Client client = resolveS3ClientDI(identification, store);
            store.addClosable(client);
            injected = client;
            Objects.requireNonNull(injected, "Unable to determine AWS S3 client.");
        } else if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = AwsIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(identification, store);
            Objects.requireNonNull(client, "Unable to determine management client.");
            store.addClosable(client);
            injected = client;
        }
        return injected;
    }
}
