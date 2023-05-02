package sunstone.core.di;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.annotation.SunstoneProperty;
import sunstone.core.SunstoneExtension;

@ExtendWith(SunstoneExtension.class)
public class SunstonePropertyTest {

    @SunstoneProperty("property")
    String property;

    @SunstoneProperty("integerProperty")
    int intPropertyConverted;

    @SunstoneProperty("integerProperty")
    Integer integerObjPropertyConverted;

    @SunstoneProperty("expressionToDefault")
    String property2;

    @SunstoneProperty(expression = "${expressionToDefault}")
    String property2Expression;

    @SunstoneProperty(expression = "prefix-${property}")
    String expression;

    @Test
    public void propertyTest() {
        Assertions.assertThat(property).isEqualTo("value");
    }

    @Test
    public void property2Test() {
        Assertions.assertThat(property2).isEqualTo("expressionDefaultValue");
        Assertions.assertThat(property2).isEqualTo(property2Expression);
    }
    @Test
    public void expression() {
        Assertions.assertThat(expression).isEqualTo("prefix-value");
    }

    @Test
    public void conversionTestPrimitiveTypeTest() {
        Assertions.assertThat(intPropertyConverted).isEqualTo(1);
    }
    @Test
    public void conversionTestObjectTest() {
        Assertions.assertThat(integerObjPropertyConverted).isEqualTo(1);
    }
}
