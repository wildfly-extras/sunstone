package azure.core;


import sunstone.api.WithAzureArmTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

public class AzureSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public SunstoneCloudDeployer create(Class annotation) {
        if (WithAzureArmTemplate.class.isAssignableFrom(annotation)) {
            return new AzureSunstoneDeployer();
        }
        return null;
    }
}
