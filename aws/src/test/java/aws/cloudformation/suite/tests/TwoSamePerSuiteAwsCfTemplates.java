package aws.cloudformation.suite.tests;


import aws.cloudformation.AwsTestUtils;
import aws.cloudformation.AwsTestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import sunstone.api.Parameter;
import aws.api.WithAwsCfTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_1)
},
        template = "aws/cloudformation/keyPair.yaml", region = "us-east-1", perSuite = true)
@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_2)
},
        template = "aws/cloudformation/keyPair.yaml", region = "us-east-1", perSuite = true)
public class TwoSamePerSuiteAwsCfTemplates {
    static Ec2Client client;

    @BeforeAll
    public static void setup() {
        client = AwsTestUtils.getEC2Client("us-east-1");
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void resourceCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(client, "tag", AwsTestConstants.TAG);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(AwsTestConstants.NAME_1);
    }
}