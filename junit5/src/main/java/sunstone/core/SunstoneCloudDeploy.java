package sunstone.core;


import com.google.common.io.BaseEncoding;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Purpose: handles creating resources on clouds. Resources may be defined by AWS CloudFormation template,
 * Azure template, JCloud Sunstone properties, ...
 * <p>
 * Used by {@link SunstoneExtension} which delegate handling TestClass annotations such as {@link WithAzureArmTemplate}.
 * Lambda function to undeploy resources is also registered for the AfterAllCallback phase.
 * <p>
 * The class works with {@link CloudDeploymentRegistry} classes such as {@link AzureArmTemplateCloudDeploymentManager}
 * that handle specific deploy method to the 3rd part cloud.
 */
class SunstoneCloudDeploy {

    private static String md5sum(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(str.getBytes("UTF-8"));
        byte[] digest = sha256.digest();
        return BaseEncoding.base16().encode(digest);
    }

    static void handleAzureArmTemplateAnnotations(ExtensionContext ctx) throws Exception {
        AzureArmTemplateCloudDeploymentManager deploymentManager = new AzureArmTemplateCloudDeploymentManager();
        SunstoneStore store = StoreWrapper(ctx);
        store.setAzureArmTemplateDeploymentManager(deploymentManager);
        AutoCloseable closeable = () -> {
            deploymentManager.undeployAll();
            deploymentManager.close();
        };
        WithAzureArmTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            String content = getResourceContent(annotations[i].template());
            Map<String, String> parameters = getParameters(annotations[i].parameters());
            deploymentManager.deployAndRegister(content, parameters);
            store.addClosable(closeable);
        }
    }

    private static Map<String, String> getParameters(Parameter[] parameters) {
        Map<String, String> parametersMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            parametersMap.put(parameters[i].k(), parameters[i].v());
        }
        parametersMap.forEach((key, value) -> parametersMap.put(key, ObjectProperties.replaceSystemProperties(value)));
        return Collections.unmodifiableMap(parametersMap);
    }

    private static String getResourceContent(String resource) throws IOException {
        InputStream is = SunstoneCloudDeploy.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = is.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
