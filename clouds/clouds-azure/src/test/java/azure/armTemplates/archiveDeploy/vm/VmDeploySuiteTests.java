package azure.armTemplates.archiveDeploy.vm;


import azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmDeployFirstTest;
import azure.armTemplates.archiveDeploy.vm.suitetests.AzureVmUndeployedSecondTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({AzureVmDeployFirstTest.class, AzureVmUndeployedSecondTest.class})
public class VmDeploySuiteTests {
    public static final String vmDeployGroup = "deploytestVM";
}
