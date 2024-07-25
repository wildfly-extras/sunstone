package sunstone.aws.impl;


import org.slf4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.s3.S3Client;

import sunstone.core.SunstoneConfigResolver;

import java.io.FileWriter;
import java.util.Optional;

class AwsUtils {
    static Logger LOGGER = AwsLogger.DEFAULT;

    private static AwsCredentialsProvider getCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(SunstoneConfigResolver.getString(AwsConfig.ACCESS_KEY_ID), SunstoneConfigResolver.getString(AwsConfig.SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    static boolean propertiesForAwsClientArePresent() {
        return SunstoneConfigResolver.unwrap().isPropertyPresent(AwsConfig.ACCESS_KEY_ID)
                && SunstoneConfigResolver.unwrap().isPropertyPresent(AwsConfig.SECRET_ACCESS_KEY);
    }

    static Region getAndCheckRegion(String regionStr) {
        Region region = Region.of(regionStr);
        if (region == null) {
            throw new IllegalArgumentException("Unkown region " + regionStr);
        }
        return region;
    }

    static CloudFormationClient getCloudFormationClient(String region) {
        CloudFormationClient cfClient = CloudFormationClient.builder()
                .region(getAndCheckRegion(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return cfClient;
    }

    static Ec2Client getEC2Client(String region) {
        Ec2Client ec2Client = Ec2Client.builder()
                .region(getAndCheckRegion(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return ec2Client;
    }

    static S3Client getS3Client(String region) {
        S3Client s3Client = S3Client.builder()
                .region(getAndCheckRegion(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return s3Client;
    }

    static RdsClient getRdsClient(String region) {
        RdsClient rdsClient = RdsClient.builder()
                .region(getAndCheckRegion(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return rdsClient;
    }

    static Optional<Instance> findEc2InstanceByNameTag(Ec2Client client, String name) {
        Filter runningInstancesFilter = Filter.builder()
                .name("instance-state-name")
                .values("running")
                .build();
        Filter tagFilter = Filter.builder()
                .name("tag:Name")
                .values(name)
                .build();

        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(runningInstancesFilter, tagFilter)
                .build();

        DescribeInstancesResponse response = client.describeInstances(request);

        for (Reservation reservation : response.reservations()) {
            for (Instance instance : reservation.instances()) {
                for (Tag tag : instance.tags()) {
                    if (tag.key().equals("Name") && tag.value().equals(name)) {
                        return Optional.of(instance);
                    }
                }
            }
        }
        return Optional.empty();
    }

    static Optional<DBInstance> findRdsInstanceByNameTag(RdsClient rdsClient, String name) {
        software.amazon.awssdk.services.rds.model.Filter nameFilter = software.amazon.awssdk.services.rds.model.Filter.builder()
                .name("db-instance-id")
                .values(name)
                .build();

        DescribeDbInstancesRequest request = DescribeDbInstancesRequest.builder()
                .filters(nameFilter).build();

        DescribeDbInstancesResponse response = rdsClient.describeDBInstances(request);

        return response.dbInstances().stream().filter(db -> db.dbInstanceStatus().equals("available")).findFirst();
    }

    static void downloadStackEvents(CloudFormationClient cfClient, String stackName) {
        try (FileWriter writer = new FileWriter("cloudformation-events-" + stackName + ".log")) {
            cfClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build()).stackEvents().forEach(event -> {
                try {
                    String log = String.format("%s\t%s\t%s\t%s",
                            event.timestamp(),
                            event.logicalResourceId(),
                            event.resourceStatus(),
                            event.resourceStatusReason());
                    writer.write(log);
                    writer.write(System.lineSeparator());
                } catch (Exception e) {
                    LOGGER.error("Error downloading stack events", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error downloading stack events", e);
        }
    }
}
