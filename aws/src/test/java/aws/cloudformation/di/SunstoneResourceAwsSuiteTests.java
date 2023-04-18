package aws.cloudformation.di;


import aws.cloudformation.di.suitetests.AwsEC2InstanceTests;
import aws.cloudformation.di.suitetests.AwsHostnameTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import static aws.cloudformation.AwsTestConstants.instanceName;

@Suite
@SelectClasses({AwsHostnameTests.class, AwsEC2InstanceTests.class})
public class SunstoneResourceAwsSuiteTests {
    public static final String suiteInstanceName = "AwsEC2InstanceTests-" + instanceName;
}
