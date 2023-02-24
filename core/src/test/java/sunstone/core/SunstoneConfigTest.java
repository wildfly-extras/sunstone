package sunstone.core;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SunstoneConfigTest {

    @BeforeAll
    static void setup() {
        System.setProperty("sunstone.test.system", "valueSet");
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("sunstone.test.system");
    }

    @BeforeEach
    public void cleanup(){
        SunstoneConfig.SunstoneExpressionSource.expressions.clear();
    }

    @Test
    public void fromPropertyFileSimple() {
        Assertions.assertThat(SunstoneConfig.getValue("property", String.class)).isEqualTo("value");
    }

    @Test
    public void fromPropertyFileExpressionResolvedToDefault() {
        Assertions.assertThat(SunstoneConfig.getValue("expressionToDefault", String.class)).isEqualTo("expressionDefaultValue");
    }

    @Test
    public void fromPropertyFileExpressionResolvedToSystemProperty() {
        Assertions.assertThat(SunstoneConfig.getValue("expressionToSystemProperty", String.class)).isEqualTo("valueSet");
    }

    @Test
    public void expressionResolved() {
        Assertions.assertThat(SunstoneConfig.resolveExpression("${expressionToSystemProperty}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfig.resolveExpression("${expressionToSystemProperty}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfig.resolveExpression("${sunstone.test.system}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfig.SunstoneExpressionSource.expressions.size()).isEqualTo(2);
    }

    @Test
    public void nestedExpressionResolved() {
        Assertions.assertThat(SunstoneConfig.resolveExpression("prefix-${expressionToSystemProperty}-${expressionToSystemProperty}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfig.resolveExpression("prefix-${expressionToSystemProperty}-${expressionToSystemProperty}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfig.resolveExpression("prefix-${sunstone.test.system}-${sunstone.test.system}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfig.SunstoneExpressionSource.expressions.size()).isEqualTo(2);
    }

    @Test
    public void resolveNotExpression() {
        Assertions.assertThat(SunstoneConfig.resolveExpression("not.a.expression", String.class)).isEqualTo("not.a.expression");
    }

    @Test
    public void isExpressionTest() {
        Assertions.assertThat(SunstoneConfig.isExpression("${expression}")).isTrue();
        Assertions.assertThat(SunstoneConfig.isExpression("xyz-${expression}-qwerty")).isTrue();
        Assertions.assertThat(SunstoneConfig.isExpression("notexpression")).isFalse();
        Assertions.assertThat(SunstoneConfig.isExpression("${notexpression")).isFalse();
        Assertions.assertThat(SunstoneConfig.isExpression("not${expression")).isFalse();
        Assertions.assertThat(SunstoneConfig.isExpression("not}expression")).isFalse();
        Assertions.assertThat(SunstoneConfig.isExpression("")).isFalse();
    }
}
