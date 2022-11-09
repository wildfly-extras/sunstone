package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class SunstoneStore {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneStore");
    private static final String CLOSABLES = "closables";
    private static final String AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER = "azureArmTemplateManager";
    private static final String AZURE_ARM_CLIENT = "azureArmClient";


    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfTemplateManager";
    private static final String AWS_CF_CLIENT = "awsCfClient";
    private final ExtensionContext context;

    private SunstoneStore(ExtensionContext ctx) {
        this.context = ctx;
    }

    static SunstoneStore StoreWrapper(ExtensionContext ctx) {
        return new SunstoneStore(ctx);
    }

    Store getStore() {
        return context.getStore(NAMESPACE);
    }

    AzureResourceManager getAzureArmClient() {
        return getStore().get(AZURE_ARM_CLIENT, AzureResourceManager.class);
    }

    void setAzureArmClient(AzureResourceManager arm) {
        getStore().put(AZURE_ARM_CLIENT, arm);
    }

    AzureArmTemplateCloudDeploymentManager getAzureArmTemplateDeploymentManager() {
        return getStore().get(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, AzureArmTemplateCloudDeploymentManager.class);
    }

    void setAzureArmTemplateDeploymentManager(AzureArmTemplateCloudDeploymentManager o) {
        getStore().put(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, o);
    }

    void setAwsCfDemploymentManager(AwsCloudFormationCloudDeploymentManager o) {
        getStore().put(AWS_CF_DEMPLOYMENT_MANAGER, o);
    }

    CloudFormationClient getAwsCfClient() {
        return getStore().get(AWS_CF_CLIENT, CloudFormationClient.class);
    }

    void setAwsCfClient(CloudFormationClient client) {
        getStore().put(AWS_CF_CLIENT, client);
    }

    void initClosables() {
        getStore().put(CLOSABLES, new ArrayDeque<>());
    }

    Deque<AutoCloseable> closables() {
        return getStore().get(CLOSABLES, Deque.class);
    }
    void addClosable(AutoCloseable closable) {
        getStore().get(CLOSABLES, Deque.class).push(closable);
    }
}
