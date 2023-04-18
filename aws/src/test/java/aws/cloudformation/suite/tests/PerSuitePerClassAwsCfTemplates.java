package aws.cloudformation.suite.tests;


import aws.cloudformation.AwsTestUtils;
import aws.cloudformation.AwsTestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneProperty;
import sunstone.aws.annotation.WithAwsCfTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_1)
},
        template = "sunstone/aws/cloudformation/keyPair.yaml", perSuite = true)
@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_2)
},
        template = "sunstone/aws/cloudformation/keyPair.yaml", perSuite = false)
public class PerSuitePerClassAwsCfTemplates {
    static Ec2Client client;

    @SunstoneProperty(expression=AwsTestConstants.NAME_1)
    static String keyName1;
    @SunstoneProperty(expression=AwsTestConstants.NAME_2)
    static String keyName2;
    @SunstoneProperty(expression=AwsTestConstants.TAG)
    static String keyTag;
    @SunstoneProperty(expression = AwsTestConstants.region)
    static String region;

    @BeforeAll
    public static void setup() {
        client = AwsTestUtils.getEC2Client(region);
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(client, "tag", keyTag);
        assertThat(keys.size()).isEqualTo(2);
        assertThat(keys).anyMatch(key -> key.keyName().equals(keyName1));
        assertThat(keys).anyMatch(key -> key.keyName().equals(keyName2));
    }
}