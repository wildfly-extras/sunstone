package aws.cloudformation.archiveDeploy.ec2Domain.suitetests;


import aws.cloudformation.archiveDeploy.ec2Domain.DomainEc2DeploySuiteTests;
import sunstone.annotation.Deployment;
import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
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
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.aws.impl.AwsWFLogger;
import sunstone.inject.Hostname;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.region;

/**
 * Deploy only to other-server-group, app shouldn't be deployed to main-server-group
 */
@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = DomainEc2DeploySuiteTests.suiteInstanceName)
},
        template = "sunstone/aws/cloudformation/eapDomain.yaml", region = region, perSuite = true)
public class AwsDomainEc2SecondDeployOtherGroupTest {
    @AwsEc2Instance(nameTag = DomainEc2DeploySuiteTests.suiteInstanceName, region = region)
    @WildFly(mode = OperatingMode.DOMAIN)
    Hostname hostname;

    @Deployment(name = "testapp.war")
    @AwsEc2Instance(nameTag = DomainEc2DeploySuiteTests.suiteInstanceName, region = region)
    @WildFly(mode = OperatingMode.DOMAIN, domain = @DomainMode(serverGroups = "other-server-group"))
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
    }

    @Test
    public void deployedOtherTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        int OTHER_SG_PORT = 8330;
        //check deployment on other-server-group
        Request request = new Request.Builder()
                .url("http://" + hostname.get() + ":" + OTHER_SG_PORT + "/testapp")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
        AwsWFLogger.DEFAULT.debug("Deployment ready on port " + OTHER_SG_PORT);
    }

    @Test
    public void undeployedMainTest() throws IOException {
        OkHttpClient client = new OkHttpClient();

        //check undeployment on main-server-group
        int[] ports = {8080, 8230};
        for (int port : ports) {
            Request request = new Request.Builder()
                    .url("http://" + hostname.get() + ":" + port + "/testapp")
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
            AwsWFLogger.DEFAULT.debug("Deployment correctly missing on port " + port);
        }
    }
}
