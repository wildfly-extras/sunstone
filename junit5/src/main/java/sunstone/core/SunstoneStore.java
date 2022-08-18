package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Deque;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class SunstoneStore {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneStore");
    private static final String CLOSABLES = "closables";

    private static final String AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER = "azureArmTemplateManager";
    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfManager";

    private static final String AZURE_ARM_CLIENT = "azureArmClient";
    private static final String AWS_CF_CLIENT = "awsCfClient";
    private static final String AWS_EC2_CLIENT = "awsEC2Client";
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

    Ec2Client getAwsEC2Client() {
        return getStore().get(AWS_EC2_CLIENT, Ec2Client.class);
    }

    void setAwsEC2Client(Ec2Client o) {
        getStore().put(AWS_EC2_CLIENT, o);
    }

    CloudFormationClient getAwsCfClient() {
        return getStore().get(AWS_EC2_CLIENT, CloudFormationClient.class);
    }

    void setAwsCfClient(CloudFormationClient o) {
        getStore().put(AWS_CF_CLIENT, o);
    }

    AzureArmTemplateCloudDeploymentManager getAzureArmTemplateDeploymentManager() {
        return getStore().get(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, AzureArmTemplateCloudDeploymentManager.class);
    }

    void setAzureArmTemplateDeploymentManager(AzureArmTemplateCloudDeploymentManager o) {
        getStore().put(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, o);
    }

    AwsCloudFormationCloudDeploymentManager getAwsCfDemploymentManager() {
        return getStore().get(AWS_CF_DEMPLOYMENT_MANAGER, AwsCloudFormationCloudDeploymentManager.class);
    }

    void setAwsCfDemploymentManager(AwsCloudFormationCloudDeploymentManager o) {
        getStore().put(AWS_CF_DEMPLOYMENT_MANAGER, o);
    }

    void setClosables(Deque<AutoCloseable> closeables) {
        getStore().put(CLOSABLES, closeables);
    }

    Deque<AutoCloseable> closables() {
        return getStore().get(CLOSABLES, Deque.class);
    }
}
