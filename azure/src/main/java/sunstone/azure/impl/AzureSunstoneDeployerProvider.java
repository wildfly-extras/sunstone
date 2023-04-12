package sunstone.azure.impl;


import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.annotation.WithAzureArmTemplateRepeatable;
import sunstone.core.AnnotationUtils;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class AzureSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Annotation annotation) {
        if (WithAzureArmTemplate.class.isAssignableFrom(annotation.annotationType())
                || WithAzureArmTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            return Optional.of(new AzureSunstoneDeployer(annotation));
        }
        Optional<WithAzureArmTemplate> transitiveSingle = AnnotationUtils.findAnnotation(annotation.annotationType(), WithAzureArmTemplate.class);
        if (transitiveSingle.isPresent()) {
            return Optional.of(new AzureSunstoneDeployer(transitiveSingle.get()));
        }
        Optional<WithAzureArmTemplateRepeatable> transitiveRepeatable = AnnotationUtils.findAnnotation(annotation.annotationType(), WithAzureArmTemplateRepeatable.class);
        if (transitiveRepeatable.isPresent()) {
            return Optional.of(new AzureSunstoneDeployer(transitiveRepeatable.get()));
        }
        return Optional.empty();
    }
}
