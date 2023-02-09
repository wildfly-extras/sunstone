package sunstone.azure.impl;


import sunstone.azure.impl.AzureIdentifiableSunstoneResource.Identification;
import sunstone.azure.annotation.AzureArchiveDeploymentAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;


public class AzureArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {

    @Override
    public Optional<SunstoneArchiveDeployer> create(Annotation annotation) {
        if (AnnotationUtils.isAnnotatedBy(annotation.annotationType(), AzureArchiveDeploymentAnnotation.class)) {
            Identification identification = new Identification(annotation);
            if (identification.type != AzureIdentifiableSunstoneResource.UNSUPPORTED && identification.type.deployToWildFlySupported()) {
                return Optional.of(new AzureArchiveDeployer());
            }
        }
        return Optional.empty();
    }
}
