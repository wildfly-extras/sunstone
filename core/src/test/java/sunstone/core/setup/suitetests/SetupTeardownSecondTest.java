package sunstone.core.setup.suitetests;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class SetupTeardownSecondTest {

    @Test
    public void test() {
        Assertions.assertThat(SetupFirstTest.StaticClassTask.teardownCalled).isTrue();
        Assertions.assertThat(RegularClassTask.teardownCalled).isTrue();
    }

    @AfterAll
    public static void reset() {
        RegularClassTask.reset();
        SetupFirstTest.StaticClassTask.reset();
    }
}
