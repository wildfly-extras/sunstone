package sunstone.aws.impl;


import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import sunstone.aws.annotation.AwsAutoResolve;
import sunstone.aws.annotation.AwsEc2Instance;
import sunstone.aws.annotation.AwsRds;
import sunstone.core.SunstoneConfigResolver;
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
 * Used by {@link AwsSunstoneResourceInjector}
 *
 * Enum of Aws resources that can be identified by {@link AwsEc2Instance} and so on and what field types are supported
 * for such identification by this module. Basically represent injection annotations and serves as a factory to get resources.
 *
 * I.e. for {@link #AUTO}, which effectively mean {@link AwsAutoResolve} is used. {@link Ec2Client}, {@link S3Client} and {@link RdsClient}
 * can be injected with such use of the annotation.
 *
 * Another example is  {@link AwsIdentifiableSunstoneResource#EC2_INSTANCE},
 * which effectively mean {@link AwsEc2Instance} is used. A user can inject {@link Hostname} and
 * {@link OnlineManagementClient}.
 */
enum AwsIdentifiableSunstoneResource {
    UNSUPPORTED(null),

    /**
     * Empty identification - e.g. {@link AwsAutoResolve}
     *
     * Injectable: {@link Ec2Client}, {@link S3Client} and {@link RdsClient}
     *
     * Deployable: archive can not be deployed to such resource
     */
    AUTO (AwsAutoResolve.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {S3Client.class, Ec2Client.class, RdsClient.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
    },

    /**
     * Aws EC2 instance identification, representation for {@link Instance}
     *
     * Injectable: {@link Hostname}
     *
     * Deployable: archive can be deployed to such resource
     */
    EC2_INSTANCE(AwsEc2Instance.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {Hostname.class, Instance.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }

        @Override
        <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            if(!getRepresentedInjectionAnnotation().isAssignableFrom(injectionAnnotation.annotationType())) {
                throw new IllegalArgumentSunstoneException(format("Expected %s annotation type but got %s",
                        getRepresentedInjectionAnnotation().getName(), injectionAnnotation.annotationType().getName()));
            }
            AwsEc2Instance vm = (AwsEc2Instance) injectionAnnotation;
            String vmNameTag = SunstoneConfigResolver.resolveExpressionToString(vm.nameTag());
            String region = SunstoneConfigResolver.resolveExpressionToString(vm.region());
            Optional<Instance> awsEc2 = AwsUtils.findEc2InstanceByNameTag(store.getAwsEc2ClientOrCreate(region), vmNameTag);
            return clazz.cast(awsEc2.orElseThrow(() -> new SunstoneCloudResourceException(format("Unable to find '%s' AWS EC2 instance in '%s' region.", vmNameTag, region))));
        }
    },

    RDS_SERVICE(AwsRds.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {Hostname.class, DBInstance.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }

        @Override
        <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            if(!getRepresentedInjectionAnnotation().isAssignableFrom(injectionAnnotation.annotationType())) {
                throw new IllegalArgumentSunstoneException(format("Expected %s annotation type but got %s",
                        getRepresentedInjectionAnnotation().getName(), injectionAnnotation.annotationType().getName()));
            }
            AwsRds rds = (AwsRds) injectionAnnotation;
            String vmNameTag = SunstoneConfigResolver.resolveExpressionToString(rds.name());
            String region = SunstoneConfigResolver.resolveExpressionToString(rds.region());
            Optional<DBInstance> awsRds = AwsUtils.findRdsInstanceByNameTag(store.getAwsRdsClientOrCreate(region), vmNameTag);
            return clazz.cast(awsRds.orElseThrow(() -> new SunstoneCloudResourceException(format("Unable to find '%s' AWS RDS instance in '%s' region.", vmNameTag, region))));
        }
    };

    private final Class<?> representedInjectionAnnotation;

    Class<?> getRepresentedInjectionAnnotation() {
        return representedInjectionAnnotation;
    }

    AwsIdentifiableSunstoneResource(Class<?> representedInjectionAnnotation) {
        this.representedInjectionAnnotation = representedInjectionAnnotation;
    }

    public String toString() {
        if (representedInjectionAnnotation == null) {
            return "unsupported AwsIdentifiableSunstoneResource type";
        } else {
            return format("%s representing %s injection annotation", this.name(), representedInjectionAnnotation.getName());
        }
    }

    boolean isTypeSupportedForInject(Class<?> type) {
        return false;
    }

    <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
        throw new UnsupportedSunstoneOperationException(format("%s annotation is not supported for the type %s",
               injectionAnnotation.annotationType().getName(), this.toString()));
    }

    public static boolean isSupported(Annotation annotation) {
        return getType(annotation) != UNSUPPORTED;
    }
    public static AwsIdentifiableSunstoneResource getType(Annotation annotation) {
        if(AwsEc2Instance.class.isAssignableFrom(annotation.annotationType())) {
            return EC2_INSTANCE;
        } else if(AwsRds.class.isAssignableFrom(annotation.annotationType())) {
            return RDS_SERVICE;
        } else if(AwsAutoResolve.class.isAssignableFrom(annotation.annotationType())) {
            return AUTO;
        } else {
            return UNSUPPORTED;
        }
    }

    /**
     * Serves as a wrapper over annotation providing {@link AwsIdentifiableSunstoneResource} type and shortcut to the
     * {@link AwsIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)} factory method.
     */
    static class Identification {
        final Annotation identification;
        final AwsIdentifiableSunstoneResource type;
        Identification(Annotation annotation) {
            this.type = AwsIdentifiableSunstoneResource.getType(annotation);
            this.identification = annotation;
        }
        <T> T get(AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return type.get(identification, store, clazz);
        }
    }

}
