package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.HashSet;
import java.util.Set;

public class SunstoneExtensionStoreHelper {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneExtension");
    private static final String DEPLOYMENT_REGISTERS = "deploymentRegisters";
    private static final String CLOSABLES = "closables";

    private static final String AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER = "azureArmTemplateManager";
    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfManager";

    private static final String AZURE_ARM_CLIENT = "azureArmClient";
    private static final String AWS_CF_CLIENT = "awsCfClient";
    private static final String AWS_EC2_CLIENT = "awsEC2Client";

    static void setupStore(ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);
        HashSet<DeploymentRegistry> deploymentRegistries = new HashSet<>();
        store.put(DEPLOYMENT_REGISTERS, deploymentRegistries);
        HashSet<AutoCloseable> closables = new HashSet<>();
        store.put(CLOSABLES, new HashSet<AutoCloseable>());
    }

    static Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    static void setAzureArmClient(ExtensionContext context, AzureResourceManager o) {
        getStore(context).put(AZURE_ARM_CLIENT, o);
    }

    static AzureResourceManager getAzureArmClient(ExtensionContext context) {
        return getStore(context).get(AZURE_ARM_CLIENT, AzureResourceManager.class);
    }

    static void setAwsCfClient(ExtensionContext context, CloudFormationClient o) {
        getStore(context).put(AWS_CF_CLIENT, o);
    }

    static Ec2Client getAwsEC2Client(ExtensionContext context) {
        return getStore(context).get(AWS_EC2_CLIENT, Ec2Client.class);
    }

    static void setAwsEC2Client(ExtensionContext context, Ec2Client o) {
        getStore(context).put(AWS_EC2_CLIENT, o);
    }

    static CloudFormationClient getAwsCfClient(ExtensionContext context) {
        return getStore(context).get(AWS_EC2_CLIENT, CloudFormationClient.class);
    }

    static void setAzureArmTemplateDeploymentManager(ExtensionContext context, AzureArmTemplateDeploymentManager o) {
        getStore(context).put(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, o);
    }

    static AzureArmTemplateDeploymentManager getAzureArmTemplateDeploymentManager(ExtensionContext context) {
        return getStore(context).get(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, AzureArmTemplateDeploymentManager.class);
    }

    static void setAwsCfDemploymentManager(ExtensionContext context, AwsCloudFormationDeploymentManager o) {
        getStore(context).put(AWS_CF_DEMPLOYMENT_MANAGER, o);
    }

    static AwsCloudFormationDeploymentManager getAwsCfDemploymentManager(ExtensionContext context) {
        return getStore(context).get(AWS_CF_DEMPLOYMENT_MANAGER, AwsCloudFormationDeploymentManager.class);
    }

    static Set<DeploymentRegistry> deploymentRegisters(ExtensionContext context) {
        return getStore(context).get(DEPLOYMENT_REGISTERS, Set.class);
    }

    static Set<AutoCloseable> closables(ExtensionContext context) {
        return getStore(context).get(CLOSABLES, Set.class);
    }
}
