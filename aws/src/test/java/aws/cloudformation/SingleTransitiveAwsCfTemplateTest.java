package aws.cloudformation;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.WithAwsCfTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SingleTransitiveAwsCfTemplateTest.TestAnnotation
public class SingleTransitiveAwsCfTemplateTest {
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
        List<KeyPairInfo> keys = AwsTestUtils.findEC2KeysByName(client, AwsTestConstants.NAME_1);
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0).keyName()).isEqualTo(AwsTestConstants.NAME_1);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @WithAwsCfTemplate(parameters = {
            @Parameter(k = "keyTag", v = AwsTestConstants.TAG),
            @Parameter(k = "keyName", v = AwsTestConstants.NAME_1)
    },
            template = "sunstone/aws/cloudformation/keyPair.yaml", region = "us-east-1")
    @interface TestAnnotation {
    }
}