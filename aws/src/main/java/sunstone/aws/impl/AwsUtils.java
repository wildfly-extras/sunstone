package sunstone.aws.impl;


import sunstone.core.properties.ObjectProperties;
import sunstone.core.properties.ObjectType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

class AwsUtils {

    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);

    private static AwsCredentialsProvider getCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(objectProperties.getProperty(AwsConfig.ACCESS_KEY_ID), objectProperties.getProperty(AwsConfig.SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    static boolean propertiesForAwsClientArePresent() {
        return objectProperties.getProperty(AwsConfig.ACCESS_KEY_ID) != null
                && objectProperties.getProperty(AwsConfig.SECRET_ACCESS_KEY) != null;
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
}
