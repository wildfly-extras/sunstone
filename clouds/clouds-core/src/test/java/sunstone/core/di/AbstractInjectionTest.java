package sunstone.core.di;


import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

@ExtendWith(SunstoneExtension.class)
public abstract class AbstractInjectionTest {

    @DirectlyAnnotatedInject
    static String directStaticInjectInAbstract;

    @DirectlyAnnotatedInject
    String directNonStaticInjectInAbstract;

    @IndirectlyAnnotatedInject
    static String indirectStaticInjectInAbstract;

    @IndirectlyAnnotatedInject
    String indirectNonStaticInjectInAbstract;
}
