package azure.armTemplates.archiveDeploy.vm.suitetests;


import azure.core.identification.AzureVirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;
import sunstone.api.inject.Hostname;

import java.io.IOException;

import static azure.armTemplates.AzureTestConstants.IMAGE_REF;
import static azure.armTemplates.AzureTestConstants.instanceName;
import static azure.armTemplates.archiveDeploy.vm.VmDeploySuiteTests.vmDeployGroup;

/**
 * The test is supposed to run after AzureWebAppDeployFirstTest and verifies undeploy operation
 */
@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = instanceName),
        @Parameter(k = "imageRefId", v = IMAGE_REF)
},
        template = "azure/armTemplates/eap.json", group = vmDeployGroup, perSuite = true)
public class AzureVmUndeployedSecondTest {
    @AzureVirtualMachine(name = instanceName, group = vmDeployGroup)
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
