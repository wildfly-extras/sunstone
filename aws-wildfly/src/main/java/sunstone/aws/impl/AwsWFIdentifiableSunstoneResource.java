package sunstone.aws.impl;


import sunstone.aws.annotation.AwsAutoResolve;
import sunstone.aws.annotation.AwsEc2Instance;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.annotation.inject.Hostname;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Used by {@link AwsWFSunstoneResourceInjector}
 *
 * Enum of Aws resources that can be identified by {@link AwsEc2Instance} and so on and what field types are supported
 * for such identification by this module. Basically represent injection annotations and serves as a factory to get resources.
 *
 * I.e. for {@link #AUTO}, which effectively mean {@link AwsAutoResolve} is used, {@link Ec2Client} and {@link S3Client}
 * can be injected with such use of the annotation.
 *
 * Another example is  {@link AwsWFIdentifiableSunstoneResource#EC2_INSTANCE},
 * which effectively mean {@link AwsEc2Instance} is used. A user can inject {@link Hostname} and
 * {@link OnlineManagementClient}.
 */
enum AwsWFIdentifiableSunstoneResource {
    UNSUPPORTED(null),

    /**
     * Empty identification - e.g. {@link AwsAutoResolve}
     *
     * Injectable: none (handled by aws module)
     *
     * Deployable: archive can not be deployed to such resource
     */
    AUTO (AwsAutoResolve.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return AwsIdentifiableSunstoneResource.AUTO.get(injectionAnnotation, store, clazz);
        }
    },

    /**
     * Aws EC2 instance identification, representation for {@link Instance}
     *
     * Injectable: {@link OnlineManagementClient}
     *
     * Deployable: archive can be deployed to such resource
     */
    EC2_INSTANCE(AwsEc2Instance.class) {
        final Class<?>[] supportedTypesForInjection = new Class[] {OnlineManagementClient.class};

        @Override
        boolean isTypeSupportedForInject(Class<?> type) {
            return Arrays.stream(supportedTypesForInjection).anyMatch(clazz -> clazz.isAssignableFrom(type));
        }
        @Override
        boolean deployToWildFlySupported() {
            return true;
        }

        @Override
        <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return AwsIdentifiableSunstoneResource.EC2_INSTANCE.get(injectionAnnotation, store, clazz);
        }
    };

    private final Class<?> representedInjectionAnnotation;

    Class<?> getRepresentedInjectionAnnotation() {
        return representedInjectionAnnotation;
    }

    AwsWFIdentifiableSunstoneResource(Class<?> representedInjectionAnnotation) {
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

    boolean deployToWildFlySupported() {
        return false;
    }
    <T> T get(Annotation injectionAnnotation, AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
        throw new UnsupportedSunstoneOperationException(format("%s annotation is not supported for the type %s",
               injectionAnnotation.annotationType().getName(), this.toString()));
    }

    public static boolean isSupported(Annotation annotation) {
        return getType(annotation) != UNSUPPORTED;
    }
    public static AwsWFIdentifiableSunstoneResource getType(Annotation annotation) {
        if(AwsEc2Instance.class.isAssignableFrom(annotation.annotationType())) {
            return EC2_INSTANCE;
        } else if(AwsAutoResolve.class.isAssignableFrom(annotation.annotationType())) {
            return AUTO;
        } else {
            return UNSUPPORTED;
        }
    }

    /**
     * Serves as a wrapper over annotation providing {@link AwsWFIdentifiableSunstoneResource} type and shortcut to the
     * {@link AwsWFIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)} factory method.
     */
    static class Identification {
        final Annotation identification;
        final AwsWFIdentifiableSunstoneResource type;
        Identification(Annotation annotation) {
            this.type = AwsWFIdentifiableSunstoneResource.getType(annotation);
            this.identification = annotation;
        }
        <T> T get(AwsSunstoneStore store, Class<T> clazz) throws SunstoneException {
            return type.get(identification, store, clazz);
        }
    }

}
