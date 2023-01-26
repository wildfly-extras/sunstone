package azure.armTemplates.archiveDeploy.webapp;


import azure.armTemplates.archiveDeploy.webapp.suitetests.AzureWebAppDeployFirstTest;
import azure.armTemplates.archiveDeploy.webapp.suitetests.AzureWebAppUndeployedSecondTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


/**
 * The order of the classes matters. The first one verify the archive is deployed. The second one doesn't deploy any and
 * verifies that undeplou operation works.
 */
@Suite
@SelectClasses({AzureWebAppDeployFirstTest.class, AzureWebAppUndeployedSecondTest.class})
public class WebAppDeploySuiteTests {
    public static final String webAppDeployGroup = "deploytestWebApp";
}
