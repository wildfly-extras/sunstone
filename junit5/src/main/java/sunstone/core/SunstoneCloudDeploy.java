package sunstone.core;


import com.google.common.io.BaseEncoding;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectPropertiesType;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import sunstone.api.AbstractParameterProvider;
import sunstone.api.ValueType;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
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
            String md5sum = md5sum(content);
            if (!annotations[i].testSuiteLevel() || !store.suiteLevelDeploymentExists(md5sum)) {
                // todo check and handle exception
                Class<? extends AbstractParameterProvider> ppClass = annotations[i].parametersProvider();
                AbstractParameterProvider parameterProvider = ppClass.getDeclaredConstructor().newInstance();
                Map<String, String> parameters = getParameters(parameterProvider, annotations[i].parameters());
                deploymentManager.deployAndRegister(content, parameters);
                store.addSuiteLevelDeployment(md5sum);
            }
        }
    }

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
        store.closables().push(() -> {
            deploymentManager.undeployAll();
            deploymentManager.close();
        });
        WithAzureArmTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            String content = getTemplateContent(annotations[i].value(), annotations[i].type());
            String md5sum = md5sum(content);
            if (!annotations[i].testSuiteLevel() || !store.suiteLevelDeploymentExists(md5sum)) {
                Class<? extends AbstractParameterProvider> ppClass = annotations[i].parametersProvider();
                // todo check and handle exception
                AbstractParameterProvider parameterProvider = ppClass.getDeclaredConstructor().newInstance();
                Map<String, String> parameters = getParameters(parameterProvider, annotations[i].parameters());
                deploymentManager.deployAndRegister(content, parameters);
                store.addSuiteLevelDeployment(md5sum);
            }
        }
    }

    private static Map<String, String> getParameters(AbstractParameterProvider parameterProvider, String[] parameters) {
        if (parameters.length % 2 != 0) {
            throw new RuntimeException("Even number of parameters are expected! First value is the key and the second one is the value.");
        }
        Map<String, String> parametersMap = parameterProvider.getParameters();
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i], parameters[i + 1]);
        }
        parametersMap.forEach((key, value) -> parametersMap.put(key, ObjectProperties.replaceSystemProperties(value)));
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
