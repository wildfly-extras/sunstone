package sunstone.aws.impl;


import sunstone.annotation.WildFly;
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
 * Used by {@link sunstone.core.SunstoneExtension} to get {@link AwsWFSunstoneResourceInjector} if it can handle the annotation
 * and the type (clazz) of field of injection operation.
 */
public class AwsWFSunstoneResourceInjectorProvider implements SunstoneResourceInjectorProvider {
    @Override
    public Optional<SunstoneResourceInjector> create(Field field) {
        Annotation[] fieldAnnotations = field.getAnnotations();
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(field.getAnnotations(), AwsResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        Optional<WildFly> wildFly = AnnotationUtils.getAnnotation(fieldAnnotations, WildFly.class);
        if (resourceIdentifications.size() > 1) {
            AwsLogger.DEFAULT.warn(format("Injected field %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", field, resourceIdentifications.get(0).toString()));
        }
        AwsWFIdentifiableSunstoneResource.Identification identification = new AwsWFIdentifiableSunstoneResource.Identification(resourceIdentifications.get(0));
        if (!wildFly.isPresent()) {
            AwsWFLogger.DEFAULT.info(format("%s is missing %s annotation, sunstone azure-wildfly is going with standalone defaults", field, WildFly.class));
        }
        if (identification.type != AwsWFIdentifiableSunstoneResource.UNSUPPORTED && identification.type.isTypeSupportedForInject(field.getType())) {
            return Optional.of(new AwsWFSunstoneResourceInjector(identification, wildFly.orElse(new WildFly.WildFlyDefault()), field.getType()));
        } else {
            return Optional.empty();
        }
    }
}
