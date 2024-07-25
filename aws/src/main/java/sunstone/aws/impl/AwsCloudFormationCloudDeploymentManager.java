package sunstone.aws.impl;


import org.slf4j.Logger;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import sunstone.core.CoreConfig;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static sunstone.core.SunstoneConfigResolver.getValue;

/**
 * Purpose: the class handles AWS CloudFormation template - deploy and undeploy the template to and from a stack.
 * <p>
 * Used by {@link AwsSunstoneDeployer}. Deploys to a stack with a random name (which is deleted as a whole later).
 * <p>
 * CloudFormation client credentials are from  Sunstone Config properties. See {@link AwsUtils}.
 */
class AwsCloudFormationCloudDeploymentManager implements Closeable {
    static Logger LOGGER = AwsLogger.DEFAULT;

    private final Map<CloudFormationClient, Set<String>> client2stacks;
    private final Map<String, CloudFormationClient> stack2Client;

    AwsCloudFormationCloudDeploymentManager() {
        client2stacks = new ConcurrentHashMap<>();
        stack2Client = new ConcurrentHashMap<>();
    }

    String deploy(CloudFormationClient cfClient, String template, Map<String, String> parameters) {
        String stackName = "SunstoneStack-" + UUID.randomUUID().toString().substring(0, 5);

        CloudFormationWaiter waiter = cfClient.waiter();

        List<Parameter> cfParameters = new ArrayList<>();
        parameters.forEach((k, v) -> cfParameters.add(Parameter.builder().parameterKey(k).parameterValue(v).build()));

        CreateStackRequest.Builder stackBuilder = CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(template)//templateURL(location)
                .parameters(cfParameters)
                .onFailure(OnFailure.ROLLBACK);

        Boolean keepOnFailure = getValue(CoreConfig.KEEP_FAILED_DEPLOY, false);
        if (keepOnFailure) {
            stackBuilder.onFailure(OnFailure.DO_NOTHING);
            LOGGER.debug("Stack will be {} preserved on failure due to {}", stackName, CoreConfig.KEEP_FAILED_DEPLOY);
        }

        CreateStackRequest stackRequest = stackBuilder.build();

        cfClient.createStack(stackRequest);
        DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                .stackName(stackName)
                .build();

        WaiterResponse<DescribeStacksResponse> waiterResponse;
        try {
            waiterResponse = waiter.waitUntilStackCreateComplete(stacksRequest);
        } catch (Exception e) {
            LOGGER.error("Stack {} failed to create", stackName);
            AwsUtils.downloadStackEvents(cfClient, stackName);
            throw e;
        }
        LOGGER.debug("Stack {} is ready {}", stackName, waiterResponse.matched().response().orElse(null));
        return stackName;
    }

    public void undeploy(String stack) {
        CloudFormationClient cfClient = stack2Client.get(stack);
        CloudFormationWaiter waiter = cfClient.waiter();

        DeleteStackRequest stackRequest = DeleteStackRequest.builder()
                .stackName(stack)
                .build();

        cfClient.deleteStack(stackRequest);
        DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                .stackName(stack)
                .build();

        WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackDeleteComplete(stacksRequest);
        LOGGER.debug("Stack {} is deleted {}", stack, waiterResponse.matched().response().orElse(null));
        stack2Client.remove(stack);
        client2stacks.get(cfClient).remove(stack);
    }

    public void close() {
        client2stacks.forEach((client, strings) -> client.close());
    }

    public String deployAndRegister(CloudFormationClient cfClient, String templateContent, Map<String, String> parameters) {
        String stack = deploy(cfClient, templateContent, parameters);
        client2stacks.putIfAbsent(cfClient, Collections.synchronizedSet(new HashSet<>()));
        client2stacks.get(cfClient).add(stack);
        stack2Client.put(stack, cfClient);
        return stack;
    }

    public void undeployAll() {
        stack2Client.forEach((stack, client) -> undeploy(stack));
    }
}
