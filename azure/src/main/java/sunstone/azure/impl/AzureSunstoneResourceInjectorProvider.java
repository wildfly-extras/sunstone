package sunstone.azure.impl;


import sunstone.azure.annotation.AzureResourceIdentificationAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AzureSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AzureSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(field.getAnnotations(), AzureResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        if (resourceIdentifications.size() > 1) {
            AzureLogger.DEFAULT.warn(format("Injected field %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", field, resourceIdentifications.get(0).toString()));
        }
        AzureIdentifiableSunstoneResource.Identification identification = new AzureIdentifiableSunstoneResource.Identification(resourceIdentifications.get(0));
        if (identification.type != AzureIdentifiableSunstoneResource.UNSUPPORTED && identification.type.isTypeSupportedForInject(field.getType())) {
            return Optional.of(new AzureSunstoneResourceInjector(identification, field.getType()));
        } else {
            return Optional.empty();
        }
    }
}
