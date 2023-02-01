package aws.cloudformation.di;


import aws.cloudformation.di.suitetests.AwsHostnameTests;
import aws.cloudformation.di.suitetests.AwsStandaloneManagementClientTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AwsHostnameTests.class, AwsStandaloneManagementClientTests.class})
public class SunstoneResourceAwsSuiteTests {
}
