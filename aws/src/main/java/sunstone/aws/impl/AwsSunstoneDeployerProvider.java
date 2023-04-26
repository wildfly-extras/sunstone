package sunstone.aws.impl;


import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.aws.annotation.WithAwsCfTemplateRepeatable;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class AwsSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Annotation annotation) {
        if (WithAwsCfTemplate.class.isAssignableFrom(annotation.annotationType())
            || WithAwsCfTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            return Optional.of(new AwsSunstoneDeployer(annotation));
        }
        Optional<WithAwsCfTemplate> transitiveSingle = AnnotationUtils.findAnnotation(annotation.annotationType(), WithAwsCfTemplate.class);
        if (transitiveSingle.isPresent()) {
            return Optional.of(new AwsSunstoneDeployer(transitiveSingle.get()));
        }
        Optional<WithAwsCfTemplateRepeatable> transitiveRepeatable = AnnotationUtils.findAnnotation(annotation.annotationType(), WithAwsCfTemplateRepeatable.class);
        if (transitiveRepeatable.isPresent()) {
            return Optional.of(new AwsSunstoneDeployer(transitiveRepeatable.get()));
        }
        return Optional.empty();
    }
}
