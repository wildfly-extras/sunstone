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

import static aws.cloudformation.AwsTestConstants.mgmtPassword;
import static aws.cloudformation.AwsTestConstants.mgmtPort;
import static aws.cloudformation.AwsTestConstants.mgmtUser;
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
    static OnlineManagementClient staticMgmtClient;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = mgmtUser, password = mgmtPassword, port = mgmtPort))
    static OnlineManagementClient staticMgmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = mgmtUser, password = mgmtPassword, port = mgmtPort))
    OnlineManagementClient mgmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName)
    OnlineManagementClient mgmtClient;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticMgmtClient).isNotNull();
        assertThat(staticMgmtClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticMgmtClient.execute(":whoami").assertSuccess();
        staticMgmtClientSpecified.execute(":whoami").assertSuccess();
        mgmtClientSpecified.execute(":whoami").assertSuccess();
        mgmtClient.execute(":whoami").assertSuccess();
    }
}