package sunstone.azure.armTemplates.archiveDeploy.vm;


import sunstone.azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmDeployFirstTest;
import sunstone.azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmUndeployedSecondTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeplou operation works.
 */
@Suite
@SelectClasses({AzureVmDeployFirstTest.class, AzureVmUndeployedSecondTest.class})
public class VmDeploySuiteTests {
    public static final String vmDeployGroup = "deploytestVM";
}
