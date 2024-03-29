package sunstone.core.archiveDeploy;


import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.annotation.Deployment;
import sunstone.core.SunstoneExtension;

import java.io.File;
import java.io.IOException;

@ExtendWith(SunstoneExtension.class)
public abstract class AbstractArchiveDeployTest {

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static File deployFileAbstract() throws IOException {
        return File.createTempFile("sunstne-test-file", "");
    }
}
