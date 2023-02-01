package sunstone.azure.armTemplates.archiveDeploy.vm.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.archiveDeploy.vm.VmDeploySuiteTests;
import sunstone.azure.api.AzureVirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.azure.api.WithAzureArmTemplate;
import sunstone.api.inject.Hostname;

import java.io.IOException;

/**
 * The test is supposed to run after AzureWebAppDeployFirstTest and verifies undeploy operation
 */
@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = VmDeploySuiteTests.vmDeployGroup, perSuite = true)
public class AzureVmUndeployedSecondTest {
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = VmDeploySuiteTests.vmDeployGroup)
    Hostname hostname;

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + hostname.get() + ":8080/testapp")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
    }
}
