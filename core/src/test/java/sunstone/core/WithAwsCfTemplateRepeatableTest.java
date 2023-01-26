package sunstone.core;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAwsCfTemplateRepeatable;

import static org.assertj.core.api.Assertions.assertThat;

@WithAwsCfTemplateRepeatable({
        @WithAwsCfTemplate(template = ""),
        @WithAwsCfTemplate(template = ""),
        @WithAwsCfTemplate(template = ""),
        @WithAwsCfTemplate(template = ""),
        @WithAwsCfTemplate(template = ""),
        @WithAwsCfTemplate(template = "")
})
public class WithAwsCfTemplateRepeatableTest {

    @AfterAll
    public static void reset() {
        TestSunstoneDeployer.reset();
    }

    @Test
    public void called() {
        assertThat(TestSunstoneDeployer.called).isTrue();
        assertThat(TestSunstoneDeployer.counter).isEqualTo(6);
    }
}
