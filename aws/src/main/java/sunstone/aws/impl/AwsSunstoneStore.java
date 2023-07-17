package sunstone.aws.impl;


import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;
import sunstone.core.SunstoneStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
class AwsSunstoneStore extends SunstoneStore {
    private static final String AWS_CF_DEMPLOYMENT_MANAGER = "awsCfTemplateManager";
    private static final String AWS_REGION_2_CF_CLIENT = "awsCfClient";
    private static final String AWS_REGION_2_EC2_CLIENT = "awsEc2Clients";
    private static final String AWS_REGION_2_RDS_CLIENT = "awsRdsClients";


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
            CloudFormationClient client = AwsUtils.getCloudFormationClient(r);
            addSuiteLevelClosable(client);
            return client;
        });
    }

    Ec2Client getAwsEc2ClientOrCreate(String regionStr) {
        ConcurrentMap<String, Ec2Client> region2Ec2Client = getStore().getOrComputeIfAbsent(AWS_REGION_2_EC2_CLIENT, s -> new ConcurrentHashMap<String, Ec2Client>(), ConcurrentMap.class);
        return  region2Ec2Client.computeIfAbsent(regionStr, r -> {
            Ec2Client client = AwsUtils.getEC2Client(r);
            addSuiteLevelClosable(client);
            return client;
        });
    }

    RdsClient getAwsRdsClientOrCreate(String regionStr) {
        ConcurrentMap<String, RdsClient> region2RdsClient = getStore().getOrComputeIfAbsent(AWS_REGION_2_RDS_CLIENT, s -> new ConcurrentHashMap<>(), ConcurrentMap.class);
        return region2RdsClient.computeIfAbsent(regionStr, r -> {
            RdsClient client = AwsUtils.getRdsClient(r);
            addSuiteLevelClosable(client);
            return client;
        });
    }
}
