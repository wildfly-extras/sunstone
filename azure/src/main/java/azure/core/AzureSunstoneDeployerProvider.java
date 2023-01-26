package azure.core;


import sunstone.api.WithAzureArmTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.util.Optional;

public class AzureSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Class annotation) {
        if (WithAzureArmTemplate.class.isAssignableFrom(annotation)) {
            return Optional.of(new AzureSunstoneDeployer());
        }
        return Optional.empty();
    }
}
