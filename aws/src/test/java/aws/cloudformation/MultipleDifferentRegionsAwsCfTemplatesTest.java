package aws.cloudformation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import sunstone.api.Parameter;
import sunstone.aws.api.WithAwsCfTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_1)
},
        template = "sunstone/aws/cloudformation/keyPair.yaml", region = "us-east-1")

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_2)
},
        template = "sunstone/aws/cloudformation/keyPair.yaml", region = "us-east-2")
public class MultipleDifferentRegionsAwsCfTemplatesTest {
    static Ec2Client clientUsEast1;
    static Ec2Client clientUsEast2;

    @BeforeAll
    public static void setup() {

        clientUsEast1 = AwsTestUtils.getEC2Client("us-east-1");
        clientUsEast2 = AwsTestUtils.getEC2Client("us-east-2");
    }

    @AfterAll
    public static void close() {
        clientUsEast1.close();
    }

    @Test
    public void usEast1resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(clientUsEast1, "tag", AwsTestConstants.TAG);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(AwsTestConstants.NAME_1);
    }
    @Test
    public void usEast2resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(clientUsEast2, "tag", AwsTestConstants.TAG);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(AwsTestConstants.NAME_2);
    }
}

