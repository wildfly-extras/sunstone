package azure.core;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import sunstone.core.TimeoutUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Optional;

public class AzureUtils {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);

    static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(objectProperties.getProperty(AzureConfig.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(objectProperties.getProperty(AzureConfig.TENANT_ID))
                .clientId(objectProperties.getProperty(AzureConfig.APPLICATION_ID))
                .clientSecret(objectProperties.getProperty(AzureConfig.PASSWORD))
                .build();
    }

    static boolean propertiesForArmClientArePresent() {
        return objectProperties.getProperty(AzureConfig.SUBSCRIPTION_ID) != null
                && objectProperties.getProperty(AzureConfig.TENANT_ID) != null
                && objectProperties.getProperty(AzureConfig.APPLICATION_ID) != null
                && objectProperties.getProperty(AzureConfig.PASSWORD) != null;
    }

    static Optional<VirtualMachine> findAzureVM(AzureResourceManager arm, String name, String resourceGroup) {
        return arm.virtualMachines().listByResourceGroup(resourceGroup).stream()
                .filter(vm -> vm.name().equals(name))
                .findAny();
    }

    static Optional<WebAppBasic> findAzureWebApp(AzureResourceManager arm, String name, String resourceGroup) {
        return arm.webApps().listByResourceGroup(resourceGroup) .stream()
                .filter(webApp -> webApp.name().equals(name))
                .findAny();
    }

    static void waitForWebApp(WebApp app) throws InterruptedException, IOException {
        OkHttpClient client = new OkHttpClient();
        // 20 minutes in millis
        long timeout = TimeoutUtils.adjust(1200000);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                Response response = client.newCall(new Request.Builder()
                                .url("http://" + app.defaultHostname())
                                .method("GET", null)
                                .build())
                        .execute();
                int code = response.code();
                /*
                Skip 404 with very specific error.
                The error comes from Azure portal when web app is already deployed but DNS has some troubles with redirecting
                the connection
                 */
                if (code == 404 && response.body() != null
                        && response.body().string().contains("404 Web Site not found.")
                        && response.body().string().contains("Custom domain has not been configured inside Azure. See <a href=\"https://go.microsoft.com/fwlink/?linkid=2194614\">how to map an existing domain</a> to resolve this.")
                        && response.body().string().contains("Client cache is still pointing the domain to old IP address. Clear the cache by running the command <i>ipconfig/flushdns.</i>")
                ) {
                    continue;
                }
                if (code < 500) {
                    return;
                }
            } catch (SocketTimeoutException e) {
                // skipping timeout
                Thread.sleep(TimeoutUtils.adjust(500));
            }
        }
        throw new RuntimeException("Timeout!");
    }
}
