package sunstone.aws.impl;


import sunstone.annotation.WildFly;
import sunstone.aws.annotation.AwsResourceIdentificationAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static sunstone.aws.impl.AwsWFIdentifiableSunstoneResource.Identification;


public class AwsWFArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {

    @Override
    public Optional<SunstoneArchiveDeployer> create(Method method) {
        Annotation[] methodAnnotations = method.getAnnotations();
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(methodAnnotations, AwsResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        Optional<WildFly> wildFly = AnnotationUtils.getAnnotation(methodAnnotations, WildFly.class);
        Identification identification = new Identification(resourceIdentifications.get(0));

        if (identification.type != AwsWFIdentifiableSunstoneResource.UNSUPPORTED && identification.type.deployToWildFlySupported()) {
            if (resourceIdentifications.size() > 1) {
                AwsWFLogger.DEFAULT.warn(format("Deployment method %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", method, resourceIdentifications.get(0).toString()));
            }
            if (!wildFly.isPresent()) {
                AwsWFLogger.DEFAULT.info(format("%s is missing %s, sunstone azure-wildfly is going with standalone defaults. ", method, WildFly.class));
            }
            return Optional.of(new AwsWFArchiveDeployer(identification, wildFly.orElse(new WildFly.WildFlyDefault())));
        }
        return Optional.empty();
    }
}
