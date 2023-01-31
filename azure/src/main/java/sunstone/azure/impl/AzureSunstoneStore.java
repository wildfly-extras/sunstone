package sunstone.azure.impl;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.SunstoneStore;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class AzureSunstoneStore extends SunstoneStore {
    private static final String AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER = "azureArmTemplateManager";
    private static final String AZURE_ARM_CLIENT = "azureArmClient";

    protected AzureSunstoneStore(ExtensionContext ctx) {
        super(ctx);
    }

    static AzureSunstoneStore get(ExtensionContext ctx) {
        return new AzureSunstoneStore(ctx);
    }

    AzureResourceManager getAzureArmClientOrCreate() {
        return getStore().getOrComputeIfAbsent(AZURE_ARM_CLIENT, s -> AzureUtils.getResourceManager(), AzureResourceManager.class);
    }

    AzureArmTemplateCloudDeploymentManager getAzureArmTemplateDeploymentManagerOrCreate() {
        return getStore().getOrComputeIfAbsent(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, k -> new AzureArmTemplateCloudDeploymentManager(getAzureArmClientOrCreate()), AzureArmTemplateCloudDeploymentManager.class);
    }
}
