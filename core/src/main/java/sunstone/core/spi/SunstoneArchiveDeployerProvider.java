package sunstone.core.spi;


import sunstone.core.api.SunstoneArchiveDeployer;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface SunstoneArchiveDeployerProvider {
    Optional<SunstoneArchiveDeployer> create(Annotation annotation);
}
