package sunstone.core.di;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.annotation.SunstoneProperty;
import sunstone.core.SunstoneExtension;

@ExtendWith(SunstoneExtension.class)
public class SunstonePropertyTest {

    @SunstoneProperty("property")
    String property = null;

    @SunstoneProperty("expressionToDefault")
    String expression;

    @Test
    public void propertyTest() {
        Assertions.assertThat(property).isEqualTo("value");
    }

    @Test
    public void expressionTest() {
        Assertions.assertThat(expression).isEqualTo("expressionDefaultValue");
    }
}
