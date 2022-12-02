package sunstone.core;


import com.google.common.io.BaseEncoding;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import sunstone.api.Parameter;
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
 * The class works with classes {@link AzureArmTemplateCloudDeploymentManager} and {@link AwsCloudFormationCloudDeploymentManager}
 * which handle deploy operation to particular cloud vendor.
 */
class SunstoneCloudDeploy {

    private static String md5sum(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(str.getBytes("UTF-8"));
        byte[] digest = sha256.digest();
        return BaseEncoding.base16().encode(digest);
    }

    static void handleAwsCloudFormationAnnotations(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        AwsCloudFormationCloudDeploymentManager deploymentManager = store.getAwsCfDemploymentManagerOrCreate();
        // lets close clients at the very end
        store.addSuiteLevelClosable(deploymentManager);
        WithAwsCfTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            WithAwsCfTemplate annotation = annotations[i];
            String content = getResourceContent(annotation.template());
            Map<String, String> parameters = getParameters(annotation.parameters());
            String region = resolveOrGetFromSunstoneProperties(annotation.region(), JUnit5Config.Aws.REGION);
            if (region == null) {
                throw new IllegalArgumentException("Region for AWS template is not defined. It must be specified either"
                        + "in the annotation or in sunstone.properties file");
            }
            String md5sum = md5sum(content);

            if (!annotation.perSuite() || !store.suiteLevelDeploymentExists(md5sum)) {
                CloudFormationClient cfClient = store.getAwsCfClientOrCreate(region);
                String stack = deploymentManager.deployAndRegister(cfClient, content, parameters);
                if (annotation.perSuite()) {
                    store.addSuiteLevelClosable(() -> deploymentManager.undeploy(stack));
                    store.addSuiteLevelDeployment(md5sum);
                } else {
                    store.addClosable(() -> deploymentManager.undeploy(stack));
                }
            }
        }
    }

    static void handleAzureArmTemplateAnnotations(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        AzureArmTemplateCloudDeploymentManager deploymentManager = store.getAzureArmTemplateDeploymentManagerOrCreate();
        WithAzureArmTemplate[] annotations = ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class);
        for (int i = 0; i < annotations.length; i++) {
            WithAzureArmTemplate annotation = annotations[i];
            String content = getResourceContent(annotation.template());
            String group = resolveOrGetFromSunstoneProperties(annotation.group(), JUnit5Config.Azure.GROUP);
            if (group == null) {
                throw new IllegalArgumentException("Resource group for Azure ARM template is not defined. "
                        + "It must be specified either in the annotation or in sunstone.properties file");
            }
            String region = resolveOrGetFromSunstoneProperties(annotation.region(), JUnit5Config.Azure.REGION);
            if (region == null) {
                throw new IllegalArgumentException("Region for AWS template is not defined. It must be specified either"
                        + "in the annotation or in sunstone.properties file");
            }

            Map<String, String> parameters = getParameters(annotation.parameters());
            String md5sum = md5sum(content);

            if (!annotation.perSuite() || !store.suiteLevelDeploymentExists(md5sum)) {
                deploymentManager.deployAndRegister(group, region, content, parameters);
                if (annotation.perSuite()) {
                    store.addSuiteLevelClosable(() -> deploymentManager.undeploy(group));
                    store.addSuiteLevelDeployment(md5sum);
                } else {
                    store.addClosable(() -> deploymentManager.undeploy(group));
                }
            }
        }
    }

    static String resolveOrGetFromSunstoneProperties(String toResolve, String sunstoneProperty) {
        String resolved = null;
        if (!toResolve.isEmpty()) {
            resolved = ObjectProperties.replaceSystemProperties(toResolve);
        } else if (sunstoneProperty != null) {
            ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);
            resolved = objectProperties.getProperty(sunstoneProperty);
        }
        return resolved;
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
        ByteArrayOutputStream result;
        try (InputStream is = SunstoneCloudDeploy.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Can not find resource " + resource);
            }
            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = is.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
