package sunstone.core.archiveDeploy;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Deployment;
import sunstone.core.di.TestSunstoneResourceInjector;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveDeployTest extends AbstractArchiveDeployTest {

    @Deployment
    @DirectlyAnnotatedArchiveDeployTarget
    @IndirectlyAnnotatedSunstoneArchiveDeployTarget
    static File deployFile() throws IOException {
        return File.createTempFile("sunstne-test-file", "");
    }


    @AfterAll
    public static void reset() {
        TestSunstoneResourceInjector.reset();
    }

    @Test
    public void test() {
        assertThat(TestSunstoneArchiveDeployer.called).isTrue();
        assertThat(TestSunstoneArchiveDeployer.counter).isEqualTo(2);

    }
}
