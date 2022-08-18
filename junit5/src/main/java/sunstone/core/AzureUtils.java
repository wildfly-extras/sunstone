package sunstone.core;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

public class AzureUtils {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);

    public static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(objectProperties.getProperty(Config.JUnit5.Azure.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(objectProperties.getProperty(Config.JUnit5.Azure.TENANT_ID))
                .clientId(objectProperties.getProperty(Config.JUnit5.Azure.APPLICATION_ID))
                .clientSecret(objectProperties.getProperty(Config.JUnit5.Azure.PASSWORD))
                .build();
    }

    static VirtualMachine findAzureVM(AzureResourceManager arm, String name, Set<String> resourceGroups) {
        for (String rg : resourceGroups) {
            VirtualMachine vm = arm.virtualMachines().getByResourceGroup(rg, name);
            if (vm != null) {
                return vm;
            }
        }
        return null;
    }

    static WebApp findAzureWebApp(AzureResourceManager arm, String name, Set<String> resourceGroups) {
        for (String rg : resourceGroups) {
            WebApp app = arm.webApps().getByResourceGroup(rg, name);
            if (app != null) {
                return app;
            }
        }
        return null;
    }

    public static void waitForWebApp(WebApp app) throws InterruptedException, IOException {
        OkHttpClient client = new OkHttpClient();
        // todo factor
        long timeout = 20 * 60 * 1000;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                Response response = client.newCall(new Request.Builder()
                                .url("http://" + app.defaultHostname())
                                .method("GET", null)
                                .build())
                        .execute();
                int code = response.code();
                if (code < 500) {
                    return;
                }
            } catch (SocketTimeoutException e) {
                // skipping timeout
                Thread.sleep(500);
            }
        }
        throw new RuntimeException("Timeout!");
    }
}
