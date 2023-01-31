package aws.cloudformation.di.suitetests;


import sunstone.aws.api.AwsEc2Instance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.EapMode;
import sunstone.api.Parameter;
import sunstone.api.StandaloneMode;
import sunstone.aws.api.WithAwsCfTemplate;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.eapMngmtPassword;
import static aws.cloudformation.AwsTestConstants.eapMngmtPort;
import static aws.cloudformation.AwsTestConstants.eapMngmtUser;
import static aws.cloudformation.AwsTestConstants.instanceName;
import static aws.cloudformation.AwsTestConstants.region;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Shared resources (EAP on AWS) among other DI tests - perSuite is true
 * Only one test case, since we don't want Sunstone to create lots of clients.
 * If you inject non-static field, they are injected per test case due to isolation and how JUnit5 framework works.
 * So the idea is to have only one testcase due to performance.
 */
@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = instanceName)
},
        template = "sunstone/aws/cloudformation/eap.yaml", region = region, perSuite = true)
public class AwsEapStandaloneManagementClientTests {

    @AwsEc2Instance(nameTag = instanceName)
    static OnlineManagementClient staticEapClient;

    @AwsEc2Instance(nameTag = instanceName, region = region, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = eapMngmtUser, password = eapMngmtPassword, port = eapMngmtPort))
    static OnlineManagementClient staticEapClientSpecified;

    @AwsEc2Instance(nameTag = instanceName, region = region, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = eapMngmtUser, password = eapMngmtPassword, port = eapMngmtPort))
    OnlineManagementClient eapClientSpecified;

    @AwsEc2Instance(nameTag = instanceName)
    OnlineManagementClient eapClient;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticEapClient).isNotNull();
        assertThat(staticEapClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticEapClient.execute(":whoami").assertSuccess();
        staticEapClientSpecified.execute(":whoami").assertSuccess();
        eapClientSpecified.execute(":whoami").assertSuccess();
        eapClient.execute(":whoami").assertSuccess();
    }
}