package sunstone.core.spi;


import sunstone.core.api.SunstoneArchiveDeployer;

import java.lang.reflect.Method;
import java.util.Optional;

public interface SunstoneArchiveDeployerProvider {
    Optional<SunstoneArchiveDeployer> create(Method method);
}
