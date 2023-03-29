package sunstone.azure.impl;


import sunstone.annotation.WildFly;
import sunstone.azure.annotation.AzureResourceIdentificationAnnotation;
import sunstone.azure.impl.AzureWFIdentifiableSunstoneResource.Identification;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;


public class AzureWFArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {

    @Override
    public Optional<SunstoneArchiveDeployer> create(Method method) {
        Annotation[] methodAnnotations = method.getAnnotations();
        List<Annotation> resourceIdentifications = AnnotationUtils.findAnnotationsAnnotatedBy(methodAnnotations, AzureResourceIdentificationAnnotation.class);
        if (resourceIdentifications.isEmpty()) {
            return Optional.empty();
        }
        Optional<WildFly> wildFly = AnnotationUtils.getAnnotation(methodAnnotations, WildFly.class);
        Identification identification = new Identification(resourceIdentifications.get(0));

        if (identification.type != AzureWFIdentifiableSunstoneResource.UNSUPPORTED && identification.type.deployToWildFlySupported()) {
            if (resourceIdentifications.size() > 1) {
                AzureWFLogger.DEFAULT.warn(format("Deployment method %s is supposed to be annotated only by one annotation identifying a cloud resource. %s is accepted.", method, resourceIdentifications.get(0).toString()));
            }
            if (identification.type == AzureWFIdentifiableSunstoneResource.WEB_APP && !wildFly.isPresent()) {
                AzureWFLogger.DEFAULT.info(format("%s is missing %s, sunstone azure-wildfly is going with standalone defaults. ", method, WildFly.class));
            }
            return Optional.of(new AzureWFArchiveDeployer(identification, wildFly.orElse(new WildFly.WildFlyDefault())));
        }
        return Optional.empty();
    }
}
