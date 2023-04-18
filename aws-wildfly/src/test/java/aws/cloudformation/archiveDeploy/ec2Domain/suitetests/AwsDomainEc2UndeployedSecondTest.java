package aws.cloudformation.archiveDeploy.ec2Domain.suitetests;


import aws.cloudformation.archiveDeploy.ec2Domain.DomainEc2DeploySuiteTests;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.WildFly;
import sunstone.aws.annotation.AwsEc2Instance;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.inject.Hostname;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.region;

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = DomainEc2DeploySuiteTests.suiteInstanceName)
},
        template = "sunstone/aws/cloudformation/eapDomain.yaml", region = region, perSuite = true)
public class AwsDomainEc2UndeployedSecondTest {
    @AwsEc2Instance(nameTag = DomainEc2DeploySuiteTests.suiteInstanceName, region = region)
    @WildFly(mode = OperatingMode.DOMAIN)
    Hostname hostname;

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        //check all servers in group
        int[] ports = {8080,8230};
        for (int port : ports) {
            Request request = new Request.Builder()
                    .url("http://" + hostname.get() + ":" + port + "/testapp")
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
        }
    }
}
