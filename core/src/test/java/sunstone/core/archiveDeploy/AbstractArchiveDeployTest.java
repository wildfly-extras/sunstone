package sunstone.core.archiveDeploy;


import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.api.Deployment;
import sunstone.core.SunstoneExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@ExtendWith(SunstoneExtension.class)
public abstract class AbstractArchiveDeployTest {

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static File deployFileAbstract() throws IOException {
        return File.createTempFile("sunstne-test-file", "");
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static Path deployPathAbstract() throws IOException {
        return (File.createTempFile("sunstne-test-file", "")).toPath();
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static Archive deployArchiveAbstract() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(AbstractArchiveDeployTest.class);
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static InputStream deployInpuStreamAbstract() {
        return InputStream.nullInputStream();
    }
}
