package aws.core;


import sunstone.api.WithAwsCfTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

public class AwsSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public SunstoneCloudDeployer create(Class annotation) {
        if (WithAwsCfTemplate.class.isAssignableFrom(annotation)) {
            return new AwsSunstoneDeployer();
        }
        return null;
    }
}
