package sunstone.core;


import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.util.Optional;

public class TestSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public Optional<SunstoneCloudDeployer> create(Class annotation) {
        return Optional.of(new TestSunstoneDeployer());
    }
}
