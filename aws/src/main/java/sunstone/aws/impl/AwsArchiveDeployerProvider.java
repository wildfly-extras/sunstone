package sunstone.aws.impl;


import sunstone.aws.annotation.AwsArchiveDeploymentAnnotation;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;


public class AwsArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {

    @Override
    public Optional<SunstoneArchiveDeployer> create(Annotation annotation) {
        if (AnnotationUtils.isAnnotatedBy(annotation.annotationType(), AwsArchiveDeploymentAnnotation.class)) {
            AwsIdentifiableSunstoneResource.Identification identification = new AwsIdentifiableSunstoneResource.Identification(annotation);
            if (identification.type != AwsIdentifiableSunstoneResource.UNSUPPORTED && identification.type.deployToWildFlySupported()) {
                return Optional.of(new AwsArchiveDeployer());
            }
        }
        return Optional.empty();
    }
}
