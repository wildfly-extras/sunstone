package sunstone.azure.impl;


import com.azure.resourcemanager.appservice.models.WebApp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sunstone.core.TimeoutUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class AzureWFUtils {
    static void waitForWebAppDeployment(WebApp app) throws InterruptedException, IOException {
        OkHttpClient client = new OkHttpClient();
        // 20 minutes in millis
        long timeout = TimeoutUtils.adjust(1200000);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                Response response = client.newCall(new Request.Builder()
                                .url("http://" + app.defaultHostname())
                                .method("GET", null)
                                .build())
                        .execute();
                int code = response.code();
                String body = response.body() != null ? response.body().string() : null;
                if (code < 500 && !isWebAppDnsProblem(code, body) && !isEapWebAppWelcomePage(code, body)) {
                    return;
                }
            } catch (SocketTimeoutException e) {
                // skipping timeout
                Thread.sleep(TimeoutUtils.adjust(500));
            }
        }
        throw new RuntimeException("Timeout!");

    }

    static void waitForWebAppCleanState(WebApp app) throws InterruptedException, IOException {
        OkHttpClient client = new OkHttpClient();
        // 20 minutes in millis
        long timeout = TimeoutUtils.adjust(1200000);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                Response response = client.newCall(new Request.Builder()
                                .url("http://" + app.defaultHostname())
                                .method("GET", null)
                                .build())
                        .execute();
                int code = response.code();
                String body = response.body() != null ? response.body().string() : null;
                // several strings from EAP Web APP welcome page
                // expecting to be any deployment undeployed and waiting for the welcome page
                if (isEapWebAppWelcomePage(code, body)) {
                    return;
                }
            } catch (SocketTimeoutException e) {
                // skipping timeout
                Thread.sleep(TimeoutUtils.adjust(500));
            }
        }
        throw new RuntimeException("Timeout!");

    }
    static void waitForWebApp(WebApp app) throws InterruptedException, IOException {
        OkHttpClient client = new OkHttpClient();
        // 20 minutes in millis
        long timeout = TimeoutUtils.adjust(1200000);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                Response response = client.newCall(new Request.Builder()
                                .url("http://" + app.defaultHostname())
                                .method("GET", null)
                                .build())
                        .execute();
                int code = response.code();
                String body = response.body() != null ? response.body().string() : null;
                if (isWebAppDnsProblem(code, body)) {
                    continue;
                }
                if (code < 500) {
                    return;
                }
            } catch (SocketTimeoutException e) {
                // skipping timeout
                Thread.sleep(TimeoutUtils.adjust(500));
            }
        }
        throw new RuntimeException("Timeout!");
    }

    /*
    Skip 404 with very specific error.
    The error comes from Azure portal when web app is already deployed but DNS has some troubles with redirecting
    the connection
     */
    private static boolean isWebAppDnsProblem(int code, String body) {
        return code == 404 && body != null
                && body.contains("404 Web Site not found.")
                && body.contains("Custom domain has not been configured inside Azure. See <a href=\"https://go.microsoft.com/fwlink/?linkid=2194614\">how to map an existing domain</a> to resolve this.")
                && body.contains("Client cache is still pointing the domain to old IP address. Clear the cache by running the command <i>ipconfig/flushdns.</i>");
    }
    private static boolean isEapWebAppWelcomePage(int code, String body) {
        return code == 200 && body != null
                && body.contains("Use deployment center to get code published from your client or setup continuous deployment.")
                && body.contains("Follow our quickstart guide and you'll have a full app ready in 5 minutes or less.<br>")
                && body.contains("<h4>Your app service is up and running.</h4>");
    }
}
