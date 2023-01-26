package azure.armTemplates;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import sunstone.core.properties.ObjectProperties;
import sunstone.core.properties.ObjectType;
import azure.core.AzureConfig;

public class AzureTestUtils {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);

    public static AzureResourceManager getResourceManager() {
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
}
