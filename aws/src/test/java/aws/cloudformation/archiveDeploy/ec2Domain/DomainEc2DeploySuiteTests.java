package aws.cloudformation.archiveDeploy.ec2Domain;


import aws.cloudformation.archiveDeploy.ec2Domain.suitetests.AwsDomainEc2DeployFirstTest;
import aws.cloudformation.archiveDeploy.ec2Domain.suitetests.AwsDomainEc2UndeployedSecondTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeploy operation works.
 */
@Suite
@SelectClasses({AwsDomainEc2DeployFirstTest.class, AwsDomainEc2UndeployedSecondTest.class})
public class DomainEc2DeploySuiteTests {
}
