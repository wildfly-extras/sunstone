package azure.armTemplates;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import sunstone.core.JUnit5Config;

public class AzureTestUtils {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);

    public static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(objectProperties.getProperty(JUnit5Config.Azure.SUBSCRIPTION_ID));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(objectProperties.getProperty(JUnit5Config.Azure.TENANT_ID))
                .clientId(objectProperties.getProperty(JUnit5Config.Azure.APPLICATION_ID))
                .clientSecret(objectProperties.getProperty(JUnit5Config.Azure.PASSWORD))
                .build();
    }
}
