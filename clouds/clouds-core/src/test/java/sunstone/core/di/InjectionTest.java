package sunstone.core.di;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InjectionTest extends AbstractInjectionTest {

    @DirectlyAnnotatedInject
    static String directStaticInject;

    @DirectlyAnnotatedInject
    String directNonStaticInject;

    @IndirectlyAnnotatedInject
    static String indirectStaticInject;

    @IndirectlyAnnotatedInject
    String indirectNonStaticInject;

    @AfterAll
    public static void reset() {
        TestSunstoneResourceInjector.reset();
    }

    @Test
    public void test() {
        assertThat(TestSunstoneResourceInjector.called).isTrue();
        assertThat(TestSunstoneResourceInjector.counter).isEqualTo(8);
        assertThat(directStaticInject).isEqualTo("set");
        assertThat(directStaticInjectInAbstract).isEqualTo("set");
        assertThat(indirectStaticInject).isEqualTo("set");
        assertThat(indirectStaticInjectInAbstract).isEqualTo("set");
        assertThat(directNonStaticInject).isEqualTo("set");
        assertThat(directNonStaticInjectInAbstract).isEqualTo("set");
        assertThat(indirectNonStaticInject).isEqualTo("set");
        assertThat(indirectNonStaticInjectInAbstract).isEqualTo("set");
    }
}
