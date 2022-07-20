package org.jboss.eapqe.clouds.ec2;

import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import sunstone.api.Sunstone;
import sunstone.api.SunstoneResource;
import sunstone.api.WithAwsCfTemplate;
import sunstone.core.AwsClientFactory;

@Sunstone
@WithAwsCfTemplate(value = "messaging-ha.yaml", parameters = {"defaultTag", "MessagingHATest2"})
public class MessagingHATest2 {

    @SunstoneResource
    Ec2Client ec2Client;

    @SunstoneResource(named = "eapqe-ha-live")
    OnlineManagementClient client;

    @Test
    public void test() {
        try {
            String nextToken = null;

            do {
                Filter runningInstancesFilter = Filter.builder()
                        .name("instance-state-name")
                        .values("running")
                        .build();
                Filter tagFilter = Filter.builder()
                        .name("tag:cf-template")
                        .values("MessagingHATest2")
                        .build();

                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .filters(runningInstancesFilter, tagFilter)
                        .build();

                DescribeInstancesResponse response = ec2Client.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        System.out.printf(
                                "Found Reservation with id %s, " +
                                        "AMI %s, " +
                                        "type %s, " +
                                        "state %s " +
                                        "and monitoring state %s\n",
                                instance.instanceId(),
                                instance.imageId(),
                                instance.instanceType(),
                                instance.state().name(),
                                instance.monitoring().state());
                    }
                }
                nextToken = response.nextToken();

            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
