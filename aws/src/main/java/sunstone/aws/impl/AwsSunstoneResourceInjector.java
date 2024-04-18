package sunstone.aws.impl;


import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import sunstone.aws.annotation.AwsAutoResolve;
import sunstone.core.SunstoneConfigResolver;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;
import sunstone.inject.Hostname;

import java.lang.annotation.Annotation;
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

    private AwsIdentifiableSunstoneResource.Identification identification;
    private Class<?> fieldType;

    public AwsSunstoneResourceInjector(AwsIdentifiableSunstoneResource.Identification identification, Class<?> fieldType) {
        this.identification = identification;
        this.fieldType = fieldType;
    }

    static Ec2Client resolveEc2ClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        Ec2Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.AUTO) {
            AwsAutoResolve annotation = (AwsAutoResolve) identification.identification;
            client = AwsUtils.getEC2Client(SunstoneConfigResolver.resolveExpressionToString(annotation.region()));
        } else {
            throw new UnsupportedSunstoneOperationException("EC2 Client may be injected only with " + AwsIdentifiableSunstoneResource.AUTO);
        }
        return client;
    }

    static S3Client resolveS3ClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        S3Client client;
        if (identification.type == AwsIdentifiableSunstoneResource.AUTO) {
            AwsAutoResolve annotation = (AwsAutoResolve) identification.identification;
            client = AwsUtils.getS3Client(SunstoneConfigResolver.resolveExpressionToString(annotation.region()));
        } else {
            throw new UnsupportedSunstoneOperationException("EC2 Client may be injected only with " + AwsIdentifiableSunstoneResource.AUTO);
        }
        return client;
    }


    private RdsClient resolveRdsClientDI(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        RdsClient client;
        if (identification.type == AwsIdentifiableSunstoneResource.AUTO) {
            AwsAutoResolve annotation = (AwsAutoResolve) identification.identification;
            client = AwsUtils.getRdsClient(SunstoneConfigResolver.resolveExpressionToString(annotation.region()));
        } else {
            throw new UnsupportedSunstoneOperationException("RDS Client may be injected only with " + AwsIdentifiableSunstoneResource.AUTO);
        }
        return client;
    }



    @Override
    public Object getResource(ExtensionContext ctx) throws SunstoneException {
        Object injected = null;
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);

        if (!identification.type.isTypeSupportedForInject(fieldType)) {
            throw new SunstoneException(format("%s is not supported for injection to %s",
                    identification.identification.annotationType(), fieldType));
        }
        if (Hostname.class.isAssignableFrom(fieldType)) {
            injected = AwsIdentifiableSunstoneResourceUtils.resolveHostname(identification, store);
            Objects.requireNonNull(injected, "Unable to determine hostname.");
        } else if (Instance.class.isAssignableFrom(fieldType)) {
            injected = identification.get(store, Instance.class);
            Objects.requireNonNull(injected, "Unable to get EC2 Instance abstraction object.");
        } else if (Ec2Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            Ec2Client client = resolveEc2ClientDI(identification, store);
            injected = client;
            Objects.requireNonNull(injected, "Unable to determine AWS EC2 client.");
        } else if (S3Client.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            S3Client client = resolveS3ClientDI(identification, store);
            injected = client;
            Objects.requireNonNull(injected, "Unable to determine AWS S3 client.");
        } else if (RdsClient.class.isAssignableFrom(fieldType)) {
            // we can inject cached client because it is not closable and a user can not change it
            RdsClient client = resolveRdsClientDI(identification, store);
            injected = client;
            Objects.requireNonNull(injected, "Unable to determine AWS RDS client.");
        } else if(DBInstance.class.isAssignableFrom(fieldType)) {
            injected = identification.get(store, DBInstance.class);
            Objects.requireNonNull(injected, "Unable to get RDS DBInstance abstraction object.");
        }
        else {
            throw new UnsupportedSunstoneOperationException("Unsupported type for injection: " + fieldType);
        }
        return injected;
    }

    @Override
    public void closeResource(Object obj) throws Exception {
        if (Hostname.class.isAssignableFrom(obj.getClass()) || Instance.class.isAssignableFrom(obj.getClass()) || DBInstance.class.isAssignableFrom(obj.getClass()) ) {
            // nothing to close
        } else if(SdkAutoCloseable.class.isAssignableFrom(obj.getClass())) {
            ((SdkAutoCloseable) obj).close();
        } else {
            throw new IllegalArgumentSunstoneException("Unknown type " + obj.getClass());
        }
    }
}
