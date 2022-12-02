package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import java.io.Closeable;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

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


    /**
     * Add {@link ExtensionContext.Store.CloseableResource} to the root global store.
     */
    void addSuiteLevelClosable(Closeable closable) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        store.put(UUID.randomUUID().toString(), (ExtensionContext.Store.CloseableResource) closable::close);
    }

    /**
     * Add sum to the root global store.
     */
    void addSuiteLevelDeployment(String checkSum) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new ConcurrentSkipListSet<String>());
        checkSums.add(checkSum);
    }

    /**
     * Check if sum is present in root global store.
     */
    boolean suiteLevelDeploymentExists(String checkSum) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new ConcurrentSkipListSet<String>());
        return checkSums.contains(checkSum);
    }

    AzureResourceManager getAzureArmClientOrCreate() {
        return getStore().getOrComputeIfAbsent(AZURE_ARM_CLIENT, s -> AzureUtils.getResourceManager(), AzureResourceManager.class);
    }

    AzureArmTemplateCloudDeploymentManager getAzureArmTemplateDeploymentManagerOrCreate() {
        return getStore().getOrComputeIfAbsent(AZURE_ARM_TEMPLATE_DEPLOYMENT_MANAGER, k -> new AzureArmTemplateCloudDeploymentManager(getAzureArmClientOrCreate()), AzureArmTemplateCloudDeploymentManager.class);
    }

    AwsCloudFormationCloudDeploymentManager getAwsCfDemploymentManagerOrCreate() {
        return getStore().getOrComputeIfAbsent(AWS_CF_DEMPLOYMENT_MANAGER, k -> new AwsCloudFormationCloudDeploymentManager(), AwsCloudFormationCloudDeploymentManager.class);
    }

    CloudFormationClient getAwsCfClientOrCreate(String regionStr) {
        ConcurrentMap<String, CloudFormationClient> region2cfClient = getStore().getOrComputeIfAbsent(AWS_REGION_2_CF_CLIENT, s -> new ConcurrentHashMap<>(), ConcurrentMap.class);
        return region2cfClient.computeIfAbsent(regionStr, AwsUtils::getCloudFormationClient);
    }

    Deque<Closeable> getClosablesOrCreate() {
        return getStore().getOrComputeIfAbsent(CLOSABLES, k -> new ConcurrentLinkedDeque<Closeable>(), Deque.class);
    }

    void addClosable(Closeable closable) {
        getClosablesOrCreate().push(closable);
    }
}
