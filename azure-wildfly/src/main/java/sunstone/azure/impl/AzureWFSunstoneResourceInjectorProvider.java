package sunstone.azure.impl;


import sunstone.annotation.WildFly;
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
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AzureWFSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AzureWFSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        Annotation[] fieldAnnotations = field.getAnnotations();
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(fieldAnnotations, AzureResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        Optional<WildFly> wildFly = AnnotationUtils.getAnnotation(fieldAnnotations, WildFly.class);
        AzureWFIdentifiableSunstoneResource.Identification identification = new AzureWFIdentifiableSunstoneResource.Identification(resourceIdentifications.get(0));
        if (identification.type != AzureWFIdentifiableSunstoneResource.UNSUPPORTED && identification.type.isTypeSupportedForInject(field.getType())) {

            if (resourceIdentifications.size() > 1) {
                AzureWFLogger.DEFAULT.warn(format("Injected field %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", field, resourceIdentifications.get(0).toString()));
            }
            if (identification.type == AzureWFIdentifiableSunstoneResource.WEB_APP && !wildFly.isPresent()) {
                AzureWFLogger.DEFAULT.info(format("%s is missing %s annotation, sunstone azure-wildfly is going with standalone defaults", field, WildFly.class));
            }
            return Optional.of(new AzureWFSunstoneResourceInjector(identification, wildFly.orElse(new WildFly.WildFlyDefault()), field.getType()));
        } else {
            return Optional.empty();
        }
    }
}
