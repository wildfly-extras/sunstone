package sunstone.core.spi;

import sunstone.core.api.SunstoneCloudDeployer;

public interface SunstoneCloudDeployerProvider {
    SunstoneCloudDeployer create(Class annotation);
}
