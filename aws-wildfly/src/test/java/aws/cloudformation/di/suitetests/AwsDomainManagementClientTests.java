package aws.cloudformation.di.suitetests;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.WildFly;
import sunstone.aws.annotation.AwsEc2Instance;
import sunstone.aws.annotation.WithAwsCfTemplate;
import static aws.cloudformation.AwsTestConstants.*;
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
        template = "sunstone/aws/cloudformation/eapDomain.yaml", region = region, perSuite = true)

public class AwsDomainManagementClientTests {

    @AwsEc2Instance(nameTag = instanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
    static OnlineManagementClient staticMngmtClient;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(
            mode = OperatingMode.DOMAIN,
            domain = @DomainMode(
                    user = mngmtUser,
                    password = mngmtPassword,
                    port = mngmtPort,
                    host = mngmtHost,
                    profile = mngmtProfile
            )
    )
    static OnlineManagementClient staticMngmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    @WildFly(
            mode = OperatingMode.DOMAIN,
            domain = @DomainMode(
                    user = mngmtUser,
                    password = mngmtPassword,
                    port = mngmtPort,
                    host = mngmtHost,
                    profile = mngmtProfile
            )
    )
    OnlineManagementClient mngmtClientSpecified;

    @AwsEc2Instance(nameTag = instanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
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
