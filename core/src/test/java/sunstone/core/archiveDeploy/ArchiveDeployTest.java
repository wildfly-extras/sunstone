package sunstone.core.archiveDeploy;


import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Deployment;
import sunstone.core.di.TestSunstoneResourceInjector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveDeployTest extends AbstractArchiveDeployTest {

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static File deployFile() throws IOException {
        return File.createTempFile("sunstne-test-file", "");
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static Path deployPath() throws IOException {
        return (File.createTempFile("sunstne-test-file", "")).toPath();
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static Archive deployArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(ArchiveDeployTest.class);
    }

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static InputStream deployInpuStream() {
        return InputStream.nullInputStream();
    }


    @AfterAll
    public static void reset() {
        TestSunstoneResourceInjector.reset();
    }

    @Test
    public void test() {
        assertThat(TestSunstoneArchiveDeployer.called).isTrue();
        assertThat(TestSunstoneArchiveDeployer.counter).isEqualTo(16);

    }
}
