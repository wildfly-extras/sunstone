package aws.cloudformation.di;


import aws.cloudformation.di.suitetests.AwsEapHostnameTests;
import aws.cloudformation.di.suitetests.AwsEapStandaloneManagementClientTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AwsEapHostnameTests.class, AwsEapStandaloneManagementClientTests.class})
public class SunstoneResourceAwsSuiteTests {
}
