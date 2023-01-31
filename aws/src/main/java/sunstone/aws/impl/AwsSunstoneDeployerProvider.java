package sunstone.aws.impl;


import sunstone.aws.api.WithAwsCfTemplate;
import sunstone.aws.api.WithAwsCfTemplateRepeatable;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class AwsSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Annotation annotation) {
        if (WithAwsCfTemplate.class.isAssignableFrom(annotation.annotationType())
            || WithAwsCfTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            return Optional.of(new AwsSunstoneDeployer());
        }
        return Optional.empty();
    }
}
