package azure.armTemplates.di;


import azure.armTemplates.di.suitetests.AzEapHostnameTests;
import azure.armTemplates.di.suitetests.AzEapStandaloneManagementClientTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AzEapHostnameTests.class, AzEapStandaloneManagementClientTests.class})
public class SunstoneResourceAzSuiteTests {
    // must be same string as in sunstone.properties
    public static final String group = "${azure.group:sunstone-testing-group}";
}
