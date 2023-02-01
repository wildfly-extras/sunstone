package sunstone.core.setup;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import sunstone.core.setup.suitetests.SetupTeardownSecondTest;
import sunstone.core.setup.suitetests.SetupFirstTest;

@Suite
@SelectClasses({SetupFirstTest.class, SetupTeardownSecondTest.class})
public class SetupSuite {
}
