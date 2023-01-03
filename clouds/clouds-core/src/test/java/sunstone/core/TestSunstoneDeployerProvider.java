package sunstone.core;


import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

public class TestSunstoneDeployerProvider implements SunstoneCloudDeployerProvider {
    @Override
    public SunstoneCloudDeployer create(Class annotation) {
        return new TestSunstoneDeployer();
    }
}
