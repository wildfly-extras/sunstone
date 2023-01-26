package sunstone.core.setup.suitetests;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class SetupCleanupSecondTest {

    @Test
    public void test() {
        Assertions.assertThat(SetupFirstTest.StaticClassTask.cleanupCalled).isTrue();
        Assertions.assertThat(RegularClassTask.cleanupCalled).isTrue();
    }

    @AfterAll
    public static void reset() {
        RegularClassTask.reset();
        SetupFirstTest.StaticClassTask.reset();
    }
}
