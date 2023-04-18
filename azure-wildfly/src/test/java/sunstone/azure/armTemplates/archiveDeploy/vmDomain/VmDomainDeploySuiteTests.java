package sunstone.azure.armTemplates.archiveDeploy.vmDomain;


import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.archiveDeploy.vmDomain.suitetests.AzureDomainVmDeployFirstTest;
import sunstone.azure.armTemplates.archiveDeploy.vmDomain.suitetests.AzureDomainVmUndeployedSecondTest;

/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeploy operation works.
 */
@Suite
@SelectClasses({AzureDomainVmDeployFirstTest.class, AzureDomainVmUndeployedSecondTest.class})
public class VmDomainDeploySuiteTests {
    public static final String groupName = "vmDomain-" + AzureTestConstants.deployGroup;
}
