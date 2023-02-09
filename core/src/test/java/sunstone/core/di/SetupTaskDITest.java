package sunstone.core.di;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Setup;

@Setup({StaticDITask.class})
public class SetupTaskDITest {

    @AfterAll
    public static void reset() {
        StaticDITask.reset();
        TestSunstoneResourceInjector.reset();
    }

    @Test
    public void test() {
        Assertions.assertThat(StaticDITask.setupCalled).isTrue();
    }

}
