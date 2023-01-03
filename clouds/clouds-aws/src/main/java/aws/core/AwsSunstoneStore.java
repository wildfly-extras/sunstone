package aws.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import sunstone.core.SunstoneStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class AwsSunstoneStore extends SunstoneStore {
    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfTemplateManager";
    private static final String AWS_REGION_2_CF_CLIENT = "awsCfClient";


    protected AwsSunstoneStore(ExtensionContext ctx) {
        super(ctx);
    }

    static AwsSunstoneStore get(ExtensionContext ctx) {
        return new AwsSunstoneStore(ctx);
    }

    AwsCloudFormationCloudDeploymentManager getAwsCfDemploymentManagerOrCreate() {
        return getStore().getOrComputeIfAbsent(AWS_CF_DEMPLOYMENT_MANAGER, k -> new AwsCloudFormationCloudDeploymentManager(), AwsCloudFormationCloudDeploymentManager.class);
    }

    CloudFormationClient getAwsCfClientOrCreate(String regionStr) {
        ConcurrentMap<String, CloudFormationClient> region2cfClient = getStore().getOrComputeIfAbsent(AWS_REGION_2_CF_CLIENT, s -> new ConcurrentHashMap<>(), ConcurrentMap.class);
        return region2cfClient.computeIfAbsent(regionStr, r -> {
            var client = AwsUtils.getCloudFormationClient(r);
            addSuiteLevelClosable(client);
            return client;
        });
    }
}
