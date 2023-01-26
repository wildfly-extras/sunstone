package aws.cloudformation.archiveDeploy.ec2.suitetests;


import aws.cloudformation.AwsTestConstants;
import aws.core.identification.AwsEc2Instance;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.inject.Hostname;

import java.io.IOException;

import static aws.cloudformation.AwsTestConstants.region;

@WithAwsCfTemplate(parameters = {
        @Parameter(k = "instanceName", v = AwsTestConstants.instanceName)
},
        template = "aws/cloudformation/eap.yaml", region = region, perSuite = true)
public class AwsEc2UndeployedSecondTest {
    @AwsEc2Instance(nameTag = AwsTestConstants.instanceName, region = region)
    Hostname hostname;

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + hostname.get() + ":8080/testapp")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isNotEqualTo("Hello World");
    }
}
