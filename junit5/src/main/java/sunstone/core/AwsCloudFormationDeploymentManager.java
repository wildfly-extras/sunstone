package sunstone.core;


import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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
import java.util.stream.Collectors;

public class AwsCloudFormationDeploymentManager implements TemplateDeploymentManager {

    private final CloudFormationClient cfClient;
    private final Set<String> stacks;

    AwsCloudFormationDeploymentManager() {
        cfClient = AwsClientFactory.getCloudFormationClient();
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
                .parameters(cfParameters
//                        parameters
//                                .entrySet()
//                                .stream()
//                                .map(entry -> Parameter.builder()
//                                        .parameterKey(entry.getKey())
//                                        .parameterValue(entry.getValue())
//                                        .build()
//                                )
//                                .collect(Collectors.toList())
                )
                .onFailure(OnFailure.ROLLBACK)
                .build();

        cfClient.createStack(stackRequest);
        DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                .stackName(stackName)
                .build();

        WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackCreateComplete(stacksRequest);
        waiterResponse.matched().response().ifPresent(System.out::println);
        System.out.println(stackName +" is ready");
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
        waiterResponse.matched().response().ifPresent(System.out::println);
        System.out.println(id + " is deleted");
    }

    @Override
    public void register(String id) {
        stacks.add(id);
    }

    public void close() {
        if (cfClient != null) {
            cfClient.close();
        }
    }

    @Override
    public void deployAndRegister(String templateContent, Map<String, String> parameters) {
        register(deploy(templateContent, parameters));
    }

    @Override
    public void undeplyAll() {
        stacks.forEach(this::undeploy);
    }
}
