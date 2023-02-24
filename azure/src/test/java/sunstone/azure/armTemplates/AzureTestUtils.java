package sunstone.azure.armTemplates;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import sunstone.azure.impl.AzureConfig;
import sunstone.core.SunstoneConfig;

public class AzureTestUtils {
    public static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(SunstoneConfig.getString(AzureConfig.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(SunstoneConfig.getString(AzureConfig.TENANT_ID))
                .clientId(SunstoneConfig.getString(AzureConfig.APPLICATION_ID))
                .clientSecret(SunstoneConfig.getString(AzureConfig.PASSWORD))
                .build();
    }
}
