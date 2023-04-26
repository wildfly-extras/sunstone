package aws.cloudformation.di.suitetests;


import sunstone.aws.annotation.AwsEc2Instance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.inject.Hostname;

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
public class AwsHostnameTests {

    @AwsEc2Instance(nameTag = instanceName)
    static Hostname staticHostname;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    static Hostname staticHostnameWithRegion;

    @AwsEc2Instance(nameTag = instanceName)
    Hostname hostname;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    Hostname hostnameWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticHostname).isNotNull();
        assertThat(staticHostnameWithRegion).isNotNull();
    }

    @Test
    public void resourceCreated() {
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(staticHostnameWithRegion.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
        assertThat(hostnameWithRegion.get()).isNotBlank();
    }
}