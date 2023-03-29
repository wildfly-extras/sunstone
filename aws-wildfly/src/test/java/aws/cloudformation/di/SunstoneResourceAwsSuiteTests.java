package aws.cloudformation.di;


import aws.cloudformation.di.suitetests.AwsStandaloneManagementClientTests;
import aws.cloudformation.di.suitetests.AwsDomainManagementClientTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AwsDomainManagementClientTests.class, AwsStandaloneManagementClientTests.class})
public class SunstoneResourceAwsSuiteTests {
}
