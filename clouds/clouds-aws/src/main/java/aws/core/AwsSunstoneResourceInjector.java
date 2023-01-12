package aws.core;


import aws.core.AwsIdentifiableSunstoneResource.Identification;
import aws.core.identification.AwsEc2Instance;
import aws.core.identification.AwsInjectionAnnotation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.s3.S3Client;
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

import static aws.core.AwsIdentifiableSunstoneResource.EC2_INSTANCE;
import static java.lang.String.format;


/**
 * Handles injecting object related to Azure cloud.
 *
 * Heavily uses {@link AwsIdentifiableSunstoneResource} to determine what should be injected into i.e. {@link Hostname}
 *
 * To retrieve Azure cloud resources, the class relies on {@link AwsIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)}.
 * If needed, it can inject resources directly or form the resources (get a hostname of AZ VM and create a {@link Hostname}) lambda
 *
 * Closable resources are registered in root extension store so that they are closed once the root store is closed (end of suite)
 */
public class AwsSunstoneResourceInjector implements SunstoneResourceInjector {
    static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);
    static Hostname resolveHostnameDI(Identification identification, AwsSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case EC2_INSTANCE:
                Instance ec2 = identification.get(store, Instance.class);
                return ec2::publicIpAddress;
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }

    static OnlineManagementClient resolveOnlineManagementClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == EC2_INSTANCE) {
                AwsEc2Instance annotation = (AwsEc2Instance) identification.identification;
                if (annotation.mode() == EapMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(resolveHostnameDI(identification, store).get(), annotation.standalone());
                } else {
                    throw new UnsupportedSunstoneOperationException("Only standalone mode is supported for injecting OnlineManagementClient.");
                }
            } else {
                throw new UnsupportedSunstoneOperationException("Only AWS EC2 instance is supported for injecting OnlineManagementClient.");
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
    }

    static Ec2Client resolveEc2ClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        Ec2Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.REGION) {
            client = AwsUtils.getEC2Client(identification.get(store, String.class));
        } else {
            client = AwsUtils.getEC2Client(objectProperties.getProperty(AwsConfig.REGION));
        }
        store.addClosable(client);
        return client;
    }

    static S3Client resolveS3ClientDI(Identification identification, AwsSunstoneStore store) throws SunstoneException {
        S3Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.REGION) {
            client = AwsUtils.getS3Client(identification.get(store, String.class));
        } else {
            client = AwsUtils.getS3Client(objectProperties.getProperty(AwsConfig.REGION));
        }
        store.addClosable(client);
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

        Identification identification = new Identification(annotation);
        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (Hostname.class.isAssignableFrom(fieldType)) {
            injected = resolveHostnameDI(identification, store);
            Objects.requireNonNull(injected, "Unable to determine hostname.");
        } else if (Ec2Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            injected = resolveEc2ClientDI(identification, store);
            Objects.requireNonNull(injected, "Unable to determine AWS EC2 client.");
        } else if (S3Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            injected = resolveS3ClientDI(identification, store);
            Objects.requireNonNull(injected, "Unable to determine AWS S3 client.");
        } else if (OnlineManagementClient.class.isAssignableFrom(fieldType)) {
            OnlineManagementClient client = resolveOnlineManagementClientDI(identification, store);
            Objects.requireNonNull(client, "Unable to determine management client.");
            store.addSuiteLevelClosable(client);
            injected = client;
        }
        return injected;
    }
}
