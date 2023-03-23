package sunstone.core.archiveDeploy;


import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public class TestSunstoneArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {
    @Override
    public Optional<SunstoneArchiveDeployer> create(Method method) {
        return Optional.of(new TestSunstoneArchiveDeployer());
    }
}
