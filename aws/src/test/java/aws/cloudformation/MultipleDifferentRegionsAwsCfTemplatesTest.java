package aws.cloudformation;

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
        template = "sunstone/aws/cloudformation/keyPair.yaml")

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
        @Parameter(k = "keyName", v = AwsTestConstants.NAME_2)
},
        template = "sunstone/aws/cloudformation/keyPair.yaml", region = AwsTestConstants.region2)
public class MultipleDifferentRegionsAwsCfTemplatesTest {
    static Ec2Client clientRegion1;
    static Ec2Client clientRegion2;

    @SunstoneProperty(expression=AwsTestConstants.NAME_1)
    static String keyName1;
    @SunstoneProperty(expression=AwsTestConstants.NAME_2)
    static String keyName2;
    @SunstoneProperty(expression=AwsTestConstants.TAG)
    static String keyTag;
    @SunstoneProperty(expression = AwsTestConstants.region)
    static String region;
    @SunstoneProperty(expression = AwsTestConstants.region2)
    static String region2;

    @BeforeAll
    public static void setup() {

        clientRegion1 = AwsTestUtils.getEC2Client(region);
        clientRegion2 = AwsTestUtils.getEC2Client(region2);
    }

    @AfterAll
    public static void close() {
        clientRegion1.close();
    }

    @Test
    public void usEast1resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(clientRegion1, "tag", keyTag);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(keyName1);
    }
    @Test
    public void usEast2resourcesCreated() {
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByTag(clientRegion2, "tag", keyTag);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(keyName2);
    }
}

