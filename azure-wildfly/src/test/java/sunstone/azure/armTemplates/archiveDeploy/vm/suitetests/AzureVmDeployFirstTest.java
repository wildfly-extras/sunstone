package sunstone.azure.armTemplates.archiveDeploy.vm.suitetests;


import sunstone.annotation.WildFly;
import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.annotation.AzureVirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.Deployment;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.annotation.inject.Hostname;

import java.io.IOException;

import static sunstone.azure.armTemplates.archiveDeploy.vm.VmDeploySuiteTests.vmDeployGroup;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = vmDeployGroup, perSuite = true)
public class AzureVmDeployFirstTest {

    @Deployment(name = "testapp.war")
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = vmDeployGroup)
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = vmDeployGroup)
    Hostname hostname;

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + hostname.get() + ":8080/testapp")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
    }
}
