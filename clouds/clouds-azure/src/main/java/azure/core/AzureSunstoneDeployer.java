package azure.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.api.WithAzureArmTemplate;
import sunstone.core.AbstractSunstoneCloudDeployer;
import sunstone.core.SunstoneExtension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Purpose: handles creating resources on clouds. Resources may be defined by AWS CloudFormation template,
 * Azure template, JCloud Sunstone properties, ...
 * <p>
 * Used by {@link SunstoneExtension} which delegate handling TestClass annotations such as {@link WithAzureArmTemplate}.
 * Lambda function to undeploy resources is also registered for the AfterAllCallback phase.
 * <p>
 * The class works with {@link AzureArmTemplateCloudDeploymentManager}
 * which handles deploy operation to particular cloud vendor.
 */
public class AzureSunstoneDeployer extends AbstractSunstoneCloudDeployer {
    @Override
    public void deploy(Annotation annotation, ExtensionContext ctx) {
        verifyClass(annotation);
        WithAzureArmTemplate armTemplateDefinition = (WithAzureArmTemplate) annotation;

        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);
        AzureArmTemplateCloudDeploymentManager deploymentManager = store.getAzureArmTemplateDeploymentManagerOrCreate();

        String content = null;
        try {
            content = getResourceContent(armTemplateDefinition.template());
            String group = resolveOrGetFromSunstoneProperties(armTemplateDefinition.group(), AzureConfig.GROUP);
            if (group == null) {
                throw new IllegalArgumentException("Resource group for Azure ARM template is not defined. "
                        + "It must be specified either in the annotation or in sunstone.properties file");
            }
            String region = resolveOrGetFromSunstoneProperties(armTemplateDefinition.region(), AzureConfig.REGION);
            if (region == null) {
                throw new IllegalArgumentException("Region for AWS template is not defined. It must be specified either"
                        + "in the annotation or in sunstone.properties file");
            }

            Map<String, String> parameters = getParameters(armTemplateDefinition.parameters());
            String md5sum = md5sum(content);

            if (!armTemplateDefinition.perSuite() || !store.suiteLevelDeploymentExists(md5sum)) {
                deploymentManager.deployAndRegister(group, region, content, parameters);
                if (armTemplateDefinition.perSuite()) {
                    store.addSuiteLevelClosable(() -> deploymentManager.undeploy(group));
                    store.addSuiteLevelDeployment(md5sum);
                } else {
                    store.addClosable(() -> deploymentManager.undeploy(group));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyClass(Annotation clazz) {
        if (!AzureUtils.propertiesForArmClientArePresent()) {
            throw new RuntimeException("Missing credentials for Azure.");
        }
    }
}
