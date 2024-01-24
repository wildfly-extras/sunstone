package aws.cloudformation.archiveDeploy.ec2Domain.suitetests;


import aws.cloudformation.archiveDeploy.ec2Domain.DomainEc2DeploySuiteTests;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.WildFly;
import sunstone.aws.annotation.AwsEc2Instance;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Deployment;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.aws.impl.AwsWFLogger;
import sunstone.inject.Hostname;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.region;

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = DomainEc2DeploySuiteTests.suiteInstanceName)
},
        template = "sunstone/aws/cloudformation/eapDomain.yaml", region = region, perSuite = true)
public class AwsDomainEc2FirstDeployAllGroupsTest {
    @AwsEc2Instance(nameTag = DomainEc2DeploySuiteTests.suiteInstanceName, region = region)
    @WildFly(mode = OperatingMode.DOMAIN)
    Hostname hostname;

    //By default, deployment should be made to all server-groups
    @Deployment(name = "testapp.war")
    @AwsEc2Instance(nameTag = DomainEc2DeploySuiteTests.suiteInstanceName, region = region)
    @WildFly(mode = OperatingMode.DOMAIN)
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        //check all servers in all server groups
        int[] ports = {8080,8230,8330};
        for (int port : ports) {
            Request request = new Request.Builder()
                    .url("http://" + hostname.get() + ":" + port + "/testapp")
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
            AwsWFLogger.DEFAULT.debug("Deployment ready on port " + port);
        }
    }
}
