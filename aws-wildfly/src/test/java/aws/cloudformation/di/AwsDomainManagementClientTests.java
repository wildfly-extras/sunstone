package aws.cloudformation.di;

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
        @Parameter(k = "instanceName", v = AwsDomainManagementClientTests.classInstanceName)
},
        template = "sunstone/aws/cloudformation/eapDomain.yaml", region = region)

public class AwsDomainManagementClientTests {
    protected static final String classInstanceName = "AwsDomainManagementClientTests-" + instanceName;

    @AwsEc2Instance(nameTag = classInstanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
    static OnlineManagementClient staticMgmtClient;

    @AwsEc2Instance(nameTag = classInstanceName, region = region)
    @WildFly(
            mode = OperatingMode.DOMAIN,
            domain = @DomainMode(
                    user = mgmtUser,
                    password = mgmtPassword,
                    port = mgmtPort,
                    host = mgmtHost,
                    profile = mgmtProfile
            )
    )
    static OnlineManagementClient staticMgmtClientSpecified;

    @AwsEc2Instance(nameTag = classInstanceName, region = region)
    @WildFly(
            mode = OperatingMode.DOMAIN,
            domain = @DomainMode(
                    user = mgmtUser,
                    password = mgmtPassword,
                    port = mgmtPort,
                    host = mgmtHost,
                    profile = mgmtProfile
            )
    )
    OnlineManagementClient mgmtClientSpecified;

    @AwsEc2Instance(nameTag = classInstanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
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
