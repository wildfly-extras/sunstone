package sunstone.core;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.api.WithAzureArmTemplate;
import sunstone.api.WithAzureArmTemplateRepeatable;

import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplateRepeatable({
        @WithAzureArmTemplate(template = ""),
        @WithAzureArmTemplate(template = ""),
        @WithAzureArmTemplate(template = ""),
        @WithAzureArmTemplate(template = ""),
        @WithAzureArmTemplate(template = "")
})
public class WitAzureArmTemplateRepeatableTest {

    @AfterAll
    public static void reset() {
        TestSunstoneDeployer.reset();
    }

    @Test
    public void called() {
        assertThat(TestSunstoneDeployer.called).isTrue();
        assertThat(TestSunstoneDeployer.counter).isEqualTo(5);
    }
}
