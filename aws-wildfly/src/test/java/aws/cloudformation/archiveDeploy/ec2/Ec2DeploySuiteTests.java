package aws.cloudformation.archiveDeploy.ec2;


import aws.cloudformation.AwsTestConstants;
import aws.cloudformation.archiveDeploy.ec2.suitetests.AwsEc2DeployFirstTest;
import aws.cloudformation.archiveDeploy.ec2.suitetests.AwsEc2UndeployedSecondTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeploy operation works.
 */
@Suite
@SelectClasses({AwsEc2DeployFirstTest.class, AwsEc2UndeployedSecondTest.class})
public class Ec2DeploySuiteTests {
    public static final String suiteInstanceName = "ec2-deploy-" + AwsTestConstants.instanceName;
}
