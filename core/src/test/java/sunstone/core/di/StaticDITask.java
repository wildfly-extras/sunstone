package sunstone.core.di;


import static org.assertj.core.api.Assertions.assertThat;

class StaticDITask extends AbstractStaticDITask {

    @DirectlyAnnotatedInject
    static String directStaticInject;

    @DirectlyAnnotatedInject
    String directNonStaticInject;

    @IndirectlyAnnotatedInject
    static String indirectStaticInject;

    @IndirectlyAnnotatedInject
    String indirectNonStaticInject;
    static boolean setupCalled = false;
    static boolean teardownCalled = false;

    @Override
    public void setup() throws Exception {
        assertThat(directStaticInject).isEqualTo("set");
        assertThat(directStaticInjectInAbstract).isEqualTo("set");
        assertThat(indirectStaticInject).isEqualTo("set");
        assertThat(indirectStaticInjectInAbstract).isEqualTo("set");
        assertThat(directNonStaticInject).isEqualTo("set");
        assertThat(directNonStaticInjectInAbstract).isEqualTo("set");
        assertThat(indirectNonStaticInject).isEqualTo("set");
        assertThat(indirectNonStaticInjectInAbstract).isEqualTo("set");
        setupCalled = true;
    }

    @Override
    public void teardown() throws Exception {
        teardownCalled = true;
    }

    public static void reset() {
        setupCalled = false;
        teardownCalled = false;
    }
}
