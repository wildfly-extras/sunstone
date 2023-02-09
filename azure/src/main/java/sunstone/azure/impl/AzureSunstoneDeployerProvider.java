package sunstone.azure.impl;


import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.annotation.WithAzureArmTemplateRepeatable;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class AzureSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Annotation annotation) {
        if (WithAzureArmTemplate.class.isAssignableFrom(annotation.annotationType())
                || WithAzureArmTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            return Optional.of(new AzureSunstoneDeployer());
        }
        return Optional.empty();
    }
}
