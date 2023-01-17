package azure.armTemplates.archiveDeploy.webapp.suitetests;


import azure.core.identification.AzureWebApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;
import sunstone.api.inject.Hostname;

import java.io.IOException;

import static azure.armTemplates.AzureTestConstants.instanceName;
import static azure.armTemplates.archiveDeploy.webapp.WebAppDeploySuiteTests.webAppDeployGroup;

@WithAzureArmTemplate(template = "azure/armTemplates/eapWebApp.json",
        parameters = {@Parameter(k = "appName", v = instanceName)}, group = webAppDeployGroup, perSuite = true)
public class AzureWebAppUndeployedSecondTest {

    @AzureWebApplication(name = instanceName, group = webAppDeployGroup)
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
