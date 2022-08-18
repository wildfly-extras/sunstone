package sunstone.core;


import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.s3.S3Client;

class AwsUtils {

    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);

    private static AwsCredentialsProvider getCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(objectProperties.getProperty(Config.JUnit5.Aws.ACCESS_KEY_ID), objectProperties.getProperty(Config.JUnit5.Aws.SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    static CloudFormationClient getCloudFormationClient() {
        CloudFormationClient cfClient = CloudFormationClient.builder()
                .region(Region.of(objectProperties.getProperty(Config.JUnit5.Aws.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return cfClient;
    }

    static Ec2Client getEC2Client() {
        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(objectProperties.getProperty(Config.JUnit5.Aws.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return ec2Client;
    }

    static S3Client getS3Client() {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(objectProperties.getProperty(Config.JUnit5.Aws.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return s3Client;
    }

    static Instance findEc2InstanceByNameTag(Ec2Client client, String name) {
        try {
            String nextToken = null;
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
                            return instance;
                        }
                    }
                }
            }
        } catch (Ec2Exception e) {
            // todo
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;

    }
}
