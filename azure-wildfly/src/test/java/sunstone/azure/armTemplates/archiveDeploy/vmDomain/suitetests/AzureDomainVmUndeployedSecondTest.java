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
import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.WildFly;
import sunstone.azure.armTemplates.archiveDeploy.vmDomain.VmDomainDeploySuiteTests;
import sunstone.inject.Hostname;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;

/**
 * The test is supposed to run after AzureWebAppDeployFirstTest and verifies undeploy operation
 */
@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "planName", v = AzureTestConstants.IMAGE_MARKETPLACE_PLAN),
        @Parameter(k = "publisher", v = AzureTestConstants.IMAGE_MARKETPLACE_PUBLISHER),
        @Parameter(k = "product", v = AzureTestConstants.IMAGE_MARKETPLACE_PRODUCT),
        @Parameter(k = "offer", v = AzureTestConstants.IMAGE_MARKETPLACE_OFFER),
        @Parameter(k = "sku", v = AzureTestConstants.IMAGE_MARKETPLACE_SKU),
        @Parameter(k = "version", v = AzureTestConstants.IMAGE_MARKETPLACE_VERSION),
},
        template = "sunstone/azure/armTemplates/eapDomain-marketplaceImage.json", group = VmDomainDeploySuiteTests.groupName, perSuite = true)
public class AzureDomainVmUndeployedSecondTest {
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = VmDomainDeploySuiteTests.groupName)
    @WildFly(mode = OperatingMode.DOMAIN)
    Hostname hostname;

    @Deployment(name = "testapp.war")
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = VmDomainDeploySuiteTests.groupName)
    @WildFly(mode = OperatingMode.DOMAIN, domain = @DomainMode(serverGroups = "other-server-group"))
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

    @Test
    public void deployedOtherTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        int OTHER_SG_PORT = 8330;
        //check deployment on other-server-group
        Request request = new Request.Builder()
                .url("http://" + hostname.get() + ":" + OTHER_SG_PORT + "/testapp")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
    }

    @Test
    public void undeployedMainTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        //check undeployment on main-server-group
        int[] ports = {8080, 8230};
        for (int port : ports) {
            Request request = new Request.Builder()
                    .url("http://" + hostname.get() + ":" + port + "/testapp")
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
        }
    }
}
