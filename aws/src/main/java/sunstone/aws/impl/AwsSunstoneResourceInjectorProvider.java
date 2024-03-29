package sunstone.aws.impl;


import sunstone.aws.annotation.AwsResourceIdentificationAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AwsSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AwsSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(field.getAnnotations(), AwsResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        if (resourceIdentifications.size() > 1) {
            AwsLogger.DEFAULT.warn(format("Injected field %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", field, resourceIdentifications.get(0).toString()));
        }
        AwsIdentifiableSunstoneResource.Identification identification = new AwsIdentifiableSunstoneResource.Identification(resourceIdentifications.get(0));
        if (identification.type != AwsIdentifiableSunstoneResource.UNSUPPORTED && identification.type.isTypeSupportedForInject(field.getType())) {
            return Optional.of(new AwsSunstoneResourceInjector(identification, field.getType()));
        } else {
            return Optional.empty();
        }
    }
}
