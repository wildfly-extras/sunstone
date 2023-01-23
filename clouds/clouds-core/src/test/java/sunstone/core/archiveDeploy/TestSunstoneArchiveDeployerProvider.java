package sunstone.core.archiveDeploy;


import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class TestSunstoneArchiveDeployerProvider implements SunstoneArchiveDeployerProvider {
    @Override
    public Optional<SunstoneArchiveDeployer> create(Annotation field) {
        return Optional.of(new TestSunstoneArchiveDeployer());
    }
}
