package sunstone.azure.armTemplates.di;


import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.inject.Hostname;
import sunstone.azure.annotation.AzureWebApplication;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(template = "sunstone/azure/armTemplates/eapWebApp.json",
        parameters = {@Parameter(k = "appName", v = AzureTestConstants.instanceName)}, group = AzureWebAppTests.group)
public class AzureWebAppTests {

    public static final String group = "sunstone-web-app";

    @AzureWebApplication(name = AzureTestConstants.instanceName, group = group)
    static Hostname staticHostname;

    @AzureWebApplication(name = AzureTestConstants.instanceName, group = group)
    static WebApp staticWebApp;

    @AzureWebApplication(name = AzureTestConstants.instanceName, group = group)
    Hostname hostname;

    @AzureWebApplication(name = AzureTestConstants.instanceName, group = group)
    WebApp webApp;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticWebApp).isNotNull();
        assertThat(staticHostname).isNotNull();
    }

    @Test
    public void testDI() {
        assertThat(staticWebApp.id()).isNotBlank();
        assertThat(webApp.id()).isNotBlank();
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
    }

    @Test
    public void test() throws IOException, InterruptedException {
        // todo we need waiters!
        waitForHttpOK(hostname, 1000 * 60 * 3);
        OkHttpClient client = new OkHttpClient();

        // todo we need preconfigured rest assured injected!
        Request request = new Request.Builder()
                .url("http://" + hostname.get())
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.code()).isEqualTo(200);
    }

    private static void waitForHttpOK(Hostname url, int timeoutMilis) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMilis) {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://" + url.get())
                        .method("GET", null)
                        .build();
                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    return;
                }
            } catch (Exception e) {
                Thread.sleep(200);
            }
        }
        Assertions.fail("timeout");
    }
}
