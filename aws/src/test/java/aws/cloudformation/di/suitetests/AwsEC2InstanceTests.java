package aws.cloudformation.di.suitetests;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.AwsEc2Instance;
import sunstone.aws.annotation.WithAwsCfTemplate;

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
public class AwsEC2InstanceTests {

    @AwsEc2Instance(nameTag = instanceName)
    static Instance staticVm;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    static Instance staticVmWithRegion;

    @AwsEc2Instance(nameTag = instanceName)
    Instance vm;

    @AwsEc2Instance(nameTag = instanceName, region = region)
    Instance vmWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticVm.instanceId()).isNotBlank();
        assertThat(staticVmWithRegion.instanceId()).isNotBlank();
    }

    @Test
    public void resourceCreated() {
        assertThat(staticVm.instanceId()).isNotBlank();
        assertThat(staticVmWithRegion.instanceId()).isNotBlank();
        assertThat(vm.instanceId()).isNotBlank();
        assertThat(vmWithRegion.instanceId()).isNotBlank();
    }
}