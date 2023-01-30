package aws.cloudformation.di.suitetests;


import aws.core.identification.AwsEc2Instance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import aws.api.WithAwsCfTemplate;
import sunstone.api.inject.Hostname;

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
        template = "aws/cloudformation/eap.yaml", region = region, perSuite = true)
public class AwsEapHostnameTests {

    @AwsEc2Instance(nameTag = instanceName)
    static Hostname staticEapHostname;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    static Hostname staticEapHostnameWithRegion;

    @AwsEc2Instance(nameTag = instanceName)
    Hostname eapHostname;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    Hostname eapHostnameWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticEapHostname).isNotNull();
        assertThat(staticEapHostnameWithRegion).isNotNull();
    }

    @Test
    public void resourceCreated() {
        assertThat(staticEapHostname.get()).isNotBlank();
        assertThat(staticEapHostnameWithRegion.get()).isNotBlank();
        assertThat(eapHostname.get()).isNotBlank();
        assertThat(eapHostnameWithRegion.get()).isNotBlank();
    }
}