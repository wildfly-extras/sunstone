package sunstone.azure.armTemplates.archiveDeploy.webapp.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.archiveDeploy.webapp.WebAppDeploySuiteTests;
import sunstone.azure.annotation.AzureWebApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.inject.Hostname;

import java.io.IOException;

/**
 * The test is supposed to run after AzureWebAppDeployFirstTest and verifies undeploy operation
 */
@WithAzureArmTemplate(template = "sunstone/azure/armTemplates/eapWebApp.json",
        parameters = {@Parameter(k = "appName", v = AzureTestConstants.instanceName)}, group = WebAppDeploySuiteTests.webAppDeployGroup, perSuite = true)
public class AzureWebAppUndeployedSecondTest {

    @AzureWebApplication(name = AzureTestConstants.instanceName, group = WebAppDeploySuiteTests.webAppDeployGroup)
    Hostname hostname;

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + hostname.get())
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
    }
}
