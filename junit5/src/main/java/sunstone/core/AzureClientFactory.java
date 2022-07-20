package sunstone.core;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

public class AzureClientFactory {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.AZURE_SDK, null);

    public static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                    .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                    .withSubscription(objectProperties.getProperty(Config.AzureSDK.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                    .tenantId(Config.AzureSDK.TENANT_ID)
                    .clientId(Config.AzureSDK.APPLICATION_ID)
                    .clientSecret(Config.AzureSDK.PASSWORD)
                    .build();
    }
}
