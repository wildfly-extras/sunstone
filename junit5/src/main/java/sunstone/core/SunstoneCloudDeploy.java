package sunstone.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.api.ValueType;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Purpose: handles creating resources on clouds. Resources may be defined by AWS CloudFormation template,
 * Azure template, JCloud Sunstone properties, ...
 *
 * Used by {@link SunstoneExtension} which delegate handling TestClass annotations such as {@link WithAzureArmTemplate}
 * or {@link WithAwsCfTemplate}. Lambda function to undeploy resources is also registered for the AfterAllCallback phase.
 *
 * The class works with {@link CloudDeploymentRegistry} classes such as {@link AwsCloudFormationCloudDeploymentManager}
 * that handle specific deploy method to the 3rd part cloud.
 */
class SunstoneCloudDeploy {

    static void handleAwsCloudFormationAnnotations(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        AwsCloudFormationCloudDeploymentManager deploymentManager = new AwsCloudFormationCloudDeploymentManager();
        store.setAwsCfDemploymentManager(deploymentManager);
        store.closables().push(() -> {
            deploymentManager.undeployAll();
            deploymentManager.close();
        });
        WithAwsCfTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            String content = getTemplateContent(annotations[i].value(), annotations[i].type());
            Map<String, String> parameters = getParameters(annotations[i].parameters());
            deploymentManager.deployAndRegister(content, parameters);
        }
    }

    static void handleAzureArmTemplateAnnotations(ExtensionContext ctx) throws Exception {
        AzureArmTemplateCloudDeploymentManager deploymentManager = new AzureArmTemplateCloudDeploymentManager();
        StoreWrapper(ctx).setAzureArmTemplateDeploymentManager(deploymentManager);
        StoreWrapper(ctx).closables().push(() -> {
            deploymentManager.undeployAll();
            deploymentManager.close();
        });
        WithAzureArmTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            String content = getTemplateContent(annotations[i].value(), annotations[i].type());
            Map<String, String> parameters = getParameters(annotations[i].parameters());
            deploymentManager.deployAndRegister(content, parameters);
        }
    }

    private static Map<String, String> getParameters(String[] parameters) {
        if (parameters.length % 2 != 0) {
            throw new RuntimeException("Even number of parameters are expected! First value is the key and the second one is the value.");
        }
        Map<String, String> parametersMap = new HashMap<>(parameters.length / 2);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i], parameters[i + 1]);
        }
        return Collections.unmodifiableMap(parametersMap);
    }

    private static String getTemplateContent(String value, ValueType type) throws IOException {
        switch (type) {
            case CONTENT:
                return value;
            case RESOURCE:
                return getResourceContent(value);
            case URL:
                return getUrlContent(value);
            default:
                throw new RuntimeException("There are no other options for template placement!");
        }
    }

    private static String getUrlContent(String url) {
        return null;
    }

    private static String getResourceContent(String resource) throws IOException {
        InputStream is = SunstoneCloudDeploy.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = is.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
