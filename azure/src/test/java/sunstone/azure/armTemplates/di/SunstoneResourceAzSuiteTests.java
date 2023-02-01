package sunstone.azure.armTemplates.di;


import sunstone.azure.armTemplates.di.suitetests.AzHostnameTests;
import sunstone.azure.armTemplates.di.suitetests.AzStandaloneManagementClientTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AzHostnameTests.class, AzStandaloneManagementClientTests.class})
public class SunstoneResourceAzSuiteTests {
    // must be same string as in sunstone.properties
    public static final String group = "${azure.group:sunstone-testing-group}";
}
