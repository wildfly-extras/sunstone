package aws.cloudformation;


import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import aws.core.AwsConfig;

import java.util.List;


public class AwsTestUtils {
    private static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);

    private static AwsCredentialsProvider getCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(objectProperties.getProperty(AwsConfig.ACCESS_KEY_ID), objectProperties.getProperty(AwsConfig.SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    static Region getAndCheckRegion(String regionStr) {
        Region region = Region.of(regionStr);
        if (region == null) {
            throw new IllegalArgumentException("Unkown region " + regionStr);
        }
        return region;
    }

    public static Ec2Client getEC2Client() {
        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.of(objectProperties.getProperty(AwsConfig.REGION)))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return ec2Client;
    }

    public static Ec2Client getEC2Client(String region) {
        Ec2Client ec2Client = Ec2Client.builder()
                .region(getAndCheckRegion(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
        return ec2Client;
    }

    public static List<KeyPairInfo> findEC2KeysByTag(Ec2Client client, String key, String value) {
        Filter tagFilter = Filter.builder()
                .name("tag:" + key)
                .values(value)
                .build();

        return describeEC2KeysOp(client, DescribeKeyPairsRequest.builder()
                .filters(tagFilter)
                .build());
    }

    public static List<KeyPairInfo> findEC2KeysByName(Ec2Client client, String name) {
        return describeEC2KeysOp(client, DescribeKeyPairsRequest.builder()
                .keyNames(name)
                .build());
    }

    public static List<KeyPairInfo> describeEC2KeysOp(Ec2Client client, DescribeKeyPairsRequest request) {
        DescribeKeyPairsResponse response = client.describeKeyPairs(request);
        return response.keyPairs();
    }
}
