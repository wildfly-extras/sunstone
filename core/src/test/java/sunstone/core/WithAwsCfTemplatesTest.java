package sunstone.core;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.api.WithAwsCfTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@WithAwsCfTemplate(template = "")
@WithAwsCfTemplate(template = "")
@WithAwsCfTemplate(template = "")
@WithAwsCfTemplate(template = "")
public class WithAwsCfTemplatesTest {

    @AfterAll
    public static void reset() {
        TestSunstoneDeployer.reset();
    }

    @Test
    public void called() {
        assertThat(TestSunstoneDeployer.called).isTrue();
        assertThat(TestSunstoneDeployer.counter).isEqualTo(4);
    }
}
