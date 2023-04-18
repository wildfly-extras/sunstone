package sunstone.azure.armTemplates.archiveDeploy.vm;


import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmDeployFirstTest;
import sunstone.azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmUndeployedSecondTest;

/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeploy operation works.
 */
@Suite
@SelectClasses({AzureVmDeployFirstTest.class, AzureVmUndeployedSecondTest.class})
public class VmDeploySuiteTests {
    public static final String groupName = "vm-" + AzureTestConstants.deployGroup;
}
