package sunstone.azure.armTemplates;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import sunstone.azure.impl.AzureConfig;
import sunstone.core.SunstoneConfigResolver;

public class AzureTestUtils {
    public static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(SunstoneConfigResolver.getString(AzureConfig.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(SunstoneConfigResolver.getString(AzureConfig.TENANT_ID))
                .clientId(SunstoneConfigResolver.getString(AzureConfig.APPLICATION_ID))
                .clientSecret(SunstoneConfigResolver.getString(AzureConfig.PASSWORD))
                .build();
    }
}
