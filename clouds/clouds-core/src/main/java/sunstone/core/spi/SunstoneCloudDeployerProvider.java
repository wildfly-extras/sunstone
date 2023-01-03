package sunstone.core.spi;

import sunstone.core.api.SunstoneCloudDeployer;

import java.util.Optional;

public interface SunstoneCloudDeployerProvider {
    Optional<SunstoneCloudDeployer> create(Class annotation);
}
