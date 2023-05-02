package org.acme.starter;


import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.azure.annotation.AzureWebApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import sunstone.inject.Hostname;
import java.io.IOException;
import sunstone.core.SunstoneExtension;

@ExtendWith(SunstoneExtension.class)
public class AzureQuarkusWebAppIT {

    @AzureWebApplication(name = QuarkusTestConstants.APP_NAME, group = QuarkusTestConstants.APP_GROUP )
    Hostname hostname;

    @Test
    public void testAvailability() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + hostname.get() + "/hello")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        Assertions.assertThat(response.body().string()).isEqualTo("Hello World");
    }

}
