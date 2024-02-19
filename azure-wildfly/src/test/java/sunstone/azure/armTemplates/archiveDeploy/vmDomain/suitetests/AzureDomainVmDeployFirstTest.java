package sunstone.azure.armTemplates.archiveDeploy.vmDomain.suitetests;


import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Deployment;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.WildFly;
import sunstone.azure.armTemplates.archiveDeploy.vmDomain.VmDomainDeploySuiteTests;
import sunstone.inject.Hostname;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "planName", v = AzureTestConstants.IMAGE_MARKETPLACE_PLAN),
        @Parameter(k = "publisher", v = AzureTestConstants.IMAGE_MARKETPLACE_PUBLISHER),
        @Parameter(k = "product", v = AzureTestConstants.IMAGE_MARKETPLACE_PRODUCT),
        @Parameter(k = "offer", v = AzureTestConstants.IMAGE_MARKETPLACE_OFFER),
        @Parameter(k = "sku", v = AzureTestConstants.IMAGE_MARKETPLACE_SKU),
        @Parameter(k = "version", v = AzureTestConstants.IMAGE_MARKETPLACE_VERSION),
},
        template = "sunstone/azure/armTemplates/eapDomain.json", group = VmDomainDeploySuiteTests.groupName, perSuite = true)
public class AzureDomainVmDeployFirstTest {
    @Deployment(name = "testapp.war")
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = VmDomainDeploySuiteTests.groupName)
    @WildFly(mode = OperatingMode.DOMAIN)
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = VmDomainDeploySuiteTests.groupName)
    @WildFly(mode = OperatingMode.DOMAIN)
    Hostname hostname;

    @Test
    public void deployedAllTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        //check all servers in group
        int[] ports = {8080,8230,8330};
        for (int port : ports) {
            Request request = new Request.Builder()
                    .url("http://" + hostname.get() + ":" + port + "/testapp")
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
        }
    }
}
