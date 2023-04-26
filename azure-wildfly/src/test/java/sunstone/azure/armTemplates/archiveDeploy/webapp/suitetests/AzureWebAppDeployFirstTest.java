package sunstone.azure.armTemplates.archiveDeploy.webapp.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.archiveDeploy.webapp.WebAppDeploySuiteTests;
import sunstone.azure.annotation.AzureWebApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Deployment;
import sunstone.annotation.Parameter;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.inject.Hostname;

import java.io.IOException;

@WithAzureArmTemplate(template = "sunstone/azure/armTemplates/eapWebApp.json",
        parameters = {@Parameter(k = "appName", v = AzureTestConstants.instanceName)}, group = WebAppDeploySuiteTests.webAppDeployGroup, perSuite = true)
public class AzureWebAppDeployFirstTest {
    @Deployment
    @AzureWebApplication(name = AzureTestConstants.instanceName, group = WebAppDeploySuiteTests.webAppDeployGroup)
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

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
        Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
    }
}
