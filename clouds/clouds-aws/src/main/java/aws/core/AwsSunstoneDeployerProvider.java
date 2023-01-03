package aws.core;


import sunstone.api.WithAwsCfTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.util.Optional;

public class AwsSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Class annotation) {
        if (WithAwsCfTemplate.class.isAssignableFrom(annotation)) {
            return Optional.of(new AwsSunstoneDeployer());
        }
        return Optional.empty();
    }
}
