package aws.cloudformation.di;


import aws.cloudformation.di.suitetests.AwsHostnameTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AwsHostnameTests.class})
public class SunstoneResourceAwsSuiteTests {
}
