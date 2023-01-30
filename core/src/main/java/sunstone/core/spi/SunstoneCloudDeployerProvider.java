package sunstone.core.spi;

import sunstone.core.api.SunstoneCloudDeployer;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface SunstoneCloudDeployerProvider {
    Optional<SunstoneCloudDeployer> create(Annotation annotation);
}
