package sunstone.core.setup.suitetests;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.annotation.AbstractSetupTask;
import sunstone.annotation.Setup;

@Setup({SetupFirstTest.StaticClassTask.class, RegularClassTask.class})
public class SetupFirstTest {

    @Test
    public void test() {
        Assertions.assertThat(StaticClassTask.setupCalled).isTrue();
        Assertions.assertThat(RegularClassTask.setupCalled).isTrue();
    }

    static class StaticClassTask extends AbstractSetupTask {
        static boolean setupCalled = false;
        static boolean teardownCalled = false;

        @Override
        public void setup() throws Exception {
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

}
