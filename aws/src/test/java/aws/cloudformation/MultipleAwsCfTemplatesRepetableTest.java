package aws.cloudformation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import sunstone.api.Parameter;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAwsCfTemplateRepeatable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WithAwsCfTemplateRepeatable({
        @WithAwsCfTemplate(parameters = {
                @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
                @Parameter(k = "keyName", v = AwsTestConstants.NAME_1)},
                template = "aws/cloudformation/keyPair.yaml", region = "us-east-1"),

        @WithAwsCfTemplate(parameters = {
                @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
                @Parameter(k = "keyName", v = AwsTestConstants.NAME_2)},
                template = "aws/cloudformation/keyPair.yaml", region = "us-east-1")
})
public class MultipleAwsCfTemplatesRepetableTest {
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
    public void resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(client, "tag", AwsTestConstants.TAG);
        assertThat(keys.size()).isEqualTo(2);
        assertThat(keys).anyMatch(key -> key.keyName().equals(AwsTestConstants.NAME_1));
        assertThat(keys).anyMatch(key -> key.keyName().equals(AwsTestConstants.NAME_2));
    }
}
