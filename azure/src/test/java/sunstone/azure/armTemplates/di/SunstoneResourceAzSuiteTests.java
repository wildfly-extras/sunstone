package sunstone.azure.armTemplates.di;


import sunstone.azure.armTemplates.di.suitetests.AzHostnameTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AzHostnameTests.class})
public class SunstoneResourceAzSuiteTests {
    // must be same string as in MP Config
    public static final String group = "${azure.group:sunstone-testing-group}";
}
