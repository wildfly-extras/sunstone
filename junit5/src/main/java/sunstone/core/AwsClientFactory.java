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

public class AwsClientFactory {

    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.AWS_SDK, null);

    private static AwsCredentialsProvider getCredentialsProvider() {
        // todo delete lab sunstone-lab user and get this from properties
        AwsBasicCredentials credentials = AwsBasicCredentials.create(objectProperties.getProperty(Config.AwsSDK.ACCESS_KEY_ID), objectProperties.getProperty(Config.AwsSDK.SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    public static CloudFormationClient getCloudFormationClient() {
        CloudFormationClient cfClient = CloudFormationClient.builder()
                .region(Region.of(objectProperties.getProperty(Config.AwsSDK.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return cfClient;
    }

    public static Ec2Client getEC2Client() {
        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(objectProperties.getProperty(Config.AwsSDK.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return ec2Client;
    }
}
