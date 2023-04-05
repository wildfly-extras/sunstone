package aws.cloudformation.di.suitetests;


import sunstone.annotation.WildFly;
import sunstone.aws.annotation.AwsEc2Instance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.StandaloneMode;
import sunstone.aws.annotation.WithAwsCfTemplate;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.mngmtPassword;
import static aws.cloudformation.AwsTestConstants.mngmtPort;
import static aws.cloudformation.AwsTestConstants.mngmtUser;
import static aws.cloudformation.AwsTestConstants.instanceName;
import static aws.cloudformation.AwsTestConstants.region;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Shared resources (WildFly on AWS) among other DI tests - perSuite is true
 * Only one test case, since we don't want Sunstone to create lots of clients.
 * If you inject non-static field, they are injected per test case due to isolation and how JUnit5 framework works.
 * So the idea is to have only one testcase due to performance.
 */
@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = instanceName)
},
        template = "sunstone/aws/cloudformation/eap.yaml", region = region, perSuite = true)
public class AwsStandaloneManagementClientTests {

    @AwsEc2Instance(nameTag = instanceName)
    static OnlineManagementClient staticMngmtClient;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = mngmtUser, password = mngmtPassword, port = mngmtPort))
    static OnlineManagementClient staticMngmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = mngmtUser, password = mngmtPassword, port = mngmtPort))
    OnlineManagementClient mngmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName)
    OnlineManagementClient mngmtClient;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticMngmtClient).isNotNull();
        assertThat(staticMngmtClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticMngmtClient.execute(":whoami").assertSuccess();
        staticMngmtClientSpecified.execute(":whoami").assertSuccess();
        mngmtClientSpecified.execute(":whoami").assertSuccess();
        mngmtClient.execute(":whoami").assertSuccess();
    }
}