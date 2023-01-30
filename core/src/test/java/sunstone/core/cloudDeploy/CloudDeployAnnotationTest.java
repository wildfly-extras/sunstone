package sunstone.core.cloudDeploy;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.core.cloudDeploy.annotations.CloudDeployAnnotation;

@CloudDeployAnnotation
public class CloudDeployAnnotationTest {

    @Test
    public void test() {
        Assertions.assertThat(TestSunstoneDeployer.called).isTrue();
    }

    @AfterAll
    public static void reset() {
        TestSunstoneDeployer.reset();
    }
}
