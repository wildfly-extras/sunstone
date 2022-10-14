package sunstone.core;


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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Purpose: the class handles AWS CloudFormation template - deploy and undeploy the template to and from a stack.
 *
 * Used by {@link SunstoneCloudDeploy}. Deploys to a stack with a random name (which is deleted as a whole later).
 *
 * CloudFormation client credentials are taken from Sunstone.properties. See {@link AwsUtils}.
 */
class AwsCloudFormationCloudDeploymentManager implements TemplateCloudDeploymentManager {
    static Logger LOGGER = SunstoneJUnit5Logger.DEFAULT;

    private final CloudFormationClient cfClient;
    private final Set<String> stacks;

    AwsCloudFormationCloudDeploymentManager() {
        cfClient = AwsUtils.getCloudFormationClient();
        stacks = new HashSet<>();
    }

    String deploy(String template, Map<String, String> parameters) {
        String stackName = "SunstoneStack-" + UUID.randomUUID().toString().substring(0,5);
        CloudFormationWaiter waiter = cfClient.waiter();

        List<Parameter> cfParameters = new ArrayList<>();
        parameters.forEach((k, v) -> cfParameters.add(Parameter.builder().parameterKey(k).parameterValue(v).build()));

        CreateStackRequest stackRequest = CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(template)//templateURL(location)
                .parameters(cfParameters)
                .onFailure(OnFailure.ROLLBACK)
                .build();

        cfClient.createStack(stackRequest);
        DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                .stackName(stackName)
                .build();

        WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackCreateComplete(stacksRequest);
        LOGGER.debug("Stack {} is ready {}",stackName, waiterResponse.matched().response().orElse(null));
        return stackName;
    }

    @Override
    public void undeploy(String id) {
        CloudFormationWaiter waiter = cfClient.waiter();

        DeleteStackRequest stackRequest = DeleteStackRequest.builder()
                .stackName(id)
                .build();

        cfClient.deleteStack(stackRequest);
        DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                .stackName(id)
                .build();

        WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackDeleteComplete(stacksRequest);
        LOGGER.debug("Stack {} is deleted {}",id, waiterResponse.matched().response().orElse(null));
    }

    @Override
    public void register(String id) {
        stacks.add(id);
    }

    public void close() {
        cfClient.close();
    }

    @Override
    public void deployAndRegister(String templateContent, Map<String, String> parameters) {
        register(deploy(templateContent, parameters));
    }

    @Override
    public void undeployAll() {
        stacks.forEach(this::undeploy);
        stacks.clear();
    }
}
