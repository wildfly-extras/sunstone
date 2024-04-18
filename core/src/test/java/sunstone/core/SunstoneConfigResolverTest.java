package sunstone.core;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SunstoneConfigResolverTest {

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
        SunstoneConfigResolver.SunstoneExpressionSource.expressions.clear();
    }

    @Test
    public void fromPropertyFileSimple() {
        Assertions.assertThat(SunstoneConfigResolver.getValue("property", String.class)).isEqualTo("value");
    }

    @Test
    public void fromPropertyFileExpressionResolvedToDefault() {
        Assertions.assertThat(SunstoneConfigResolver.getValue("expressionToDefault", String.class)).isEqualTo("expressionDefaultValue");
    }

    @Test
    public void fromPropertyFileExpressionResolvedToSystemProperty() {
        Assertions.assertThat(SunstoneConfigResolver.getValue("expressionToSystemProperty", String.class)).isEqualTo("valueSet");
    }

    @Test
    public void expressionResolved() {
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("${expressionToSystemProperty}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("${expressionToSystemProperty}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("${sunstone.test.system}", String.class)).isEqualTo("valueSet");
        Assertions.assertThat(SunstoneConfigResolver.SunstoneExpressionSource.expressions.size()).isEqualTo(2);
    }

    @Test
    public void nestedExpressionResolved() {
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("prefix-${expressionToSystemProperty}-${expressionToSystemProperty}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("prefix-${expressionToSystemProperty}-${expressionToSystemProperty}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("prefix-${sunstone.test.system}-${sunstone.test.system}-suffix", String.class)).isEqualTo("prefix-valueSet-valueSet-suffix");
        Assertions.assertThat(SunstoneConfigResolver.SunstoneExpressionSource.expressions.size()).isEqualTo(2);
    }

    @Test
    public void resolveNotExpression() {
        Assertions.assertThat(SunstoneConfigResolver.resolveExpression("not.a.expression", String.class)).isEqualTo("not.a.expression");
    }

    @Test
    public void isExpressionTest() {
        Assertions.assertThat(SunstoneConfigResolver.isExpression("${expression}")).isTrue();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("xyz-${expression}-qwerty")).isTrue();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("notexpression")).isFalse();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("${notexpression")).isFalse();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("not${expression")).isFalse();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("not}expression")).isFalse();
        Assertions.assertThat(SunstoneConfigResolver.isExpression("")).isFalse();
    }
}
