package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class SunstoneStore {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneStore");
    private static final String CLOSABLES = "closables";
    private static final String AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER = "azureArmTemplateManager";
    private static final String AZURE_ARM_CLIENT = "azureArmClient";


    private static final String SUITE_LEVEL_DEPLOYMENTS = "sunstoneSuiteLevelDeployments";


    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfTemplateManager";
    private static final String AWS_REGION_2_CF_CLIENT = "awsCfClient";
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


    void addSuiteLevelClosable(AutoCloseable closable) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        store.put(UUID.randomUUID().toString(), (ExtensionContext.Store.CloseableResource) closable::close);
    }
    void addSuiteLevelDeployment(String checkSum) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new HashSet<String>());
        checkSums.add(checkSum);
    }
    boolean suiteLevelDeploymentExists(String checkSum) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new HashSet<String>());
        return checkSums.contains(checkSum);
    }

    AzureResourceManager getAzureArmClientOrCreate() {
        return getStore().getOrComputeIfAbsent(AZURE_ARM_CLIENT, s -> AzureUtils.getResourceManager(), AzureResourceManager.class);
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

    CloudFormationClient getAwsCfClientOrCreate(String regionStr) {
        Region region = AwsUtils.getAndCheckRegion(regionStr);
        Map<Region, CloudFormationClient> region2cfClient = getStore().getOrComputeIfAbsent(AWS_REGION_2_CF_CLIENT, s -> new HashMap<Region, CloudFormationClient>(), Map.class);
        CloudFormationClient cfClient = region2cfClient.get(region);
        if (cfClient == null) {
            cfClient = AwsUtils.getCloudFormationClient(regionStr);
            region2cfClient.put(region, cfClient);
        }
        return cfClient;
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
