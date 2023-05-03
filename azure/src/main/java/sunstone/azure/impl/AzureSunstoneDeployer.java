package sunstone.azure.impl;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.annotation.WithAzureArmTemplateRepeatable;
import sunstone.core.AbstractSunstoneCloudDeployer;
import sunstone.core.SunstoneConfig;
import sunstone.core.SunstoneExtension;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static java.lang.String.format;

/**
 * Purpose: handles creating resources on clouds. Resources may be defined by Azure ARM template
 * <p>
 * Used by {@link SunstoneExtension} which delegate handling TestClass annotations such as {@link WithAzureArmTemplate}.
 * Lambda function to undeploy resources is also registered for the AfterAllCallback phase.
 * <p>
 * The class works with {@link AzureArmTemplateCloudDeploymentManager}
 * which handles deploy operation to particular cloud vendor.
 */
public class AzureSunstoneDeployer extends AbstractSunstoneCloudDeployer {

    private final Annotation annotation;

    AzureSunstoneDeployer(Annotation annotation) {
        this.annotation = annotation;
    }

    @Override
    public void deployAndRegisterForUndeploy(ExtensionContext ctx) throws SunstoneException {
        verify(annotation);
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);
        if (WithAzureArmTemplate.class.isAssignableFrom(annotation.annotationType())) {
            deployArmTemplate((WithAzureArmTemplate) annotation, store);
        } else if (WithAzureArmTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            for (WithAzureArmTemplate withAzureArmTemplate : ((WithAzureArmTemplateRepeatable) annotation).value()) {
                deployArmTemplate(withAzureArmTemplate, store);
            }
        }
    }

    private void deployArmTemplate(WithAzureArmTemplate armTemplateDefinition, AzureSunstoneStore store) throws SunstoneException {
        AzureArmTemplateCloudDeploymentManager deploymentManager = store.getAzureArmTemplateDeploymentManagerOrCreate();

        String content = null;
        try {
            content = getResourceContent(armTemplateDefinition.template());
            String group = SunstoneConfig.resolveExpressionToString(armTemplateDefinition.group());
            if (group == null) {
                throw new IllegalArgumentSunstoneException("Resource group for Azure ARM template is not defined. "
                        + "It must be specified either in the annotation or as Sunstone Config property.");
            }
            String region = SunstoneConfig.resolveExpressionToString(armTemplateDefinition.region());
            if (region == null) {
                throw new IllegalArgumentSunstoneException("Region for Azure ARM template is not defined. It must be specified either "
                        + "in the annotation or as Sunstone Config property.");
            }

            Map<String, String> parameters = getParameters(armTemplateDefinition.parameters());
            String md5sum = sum(content);

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
            throw new SunstoneException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SunstoneException(e);
        }
    }

    private void verify(Annotation clazz) throws IllegalArgumentSunstoneException {
        if (!AzureUtils.propertiesForArmClientArePresent()){
            throw new IllegalArgumentSunstoneException("Missing credentials for Azure.");
        }
        if (!WithAzureArmTemplate.class.isAssignableFrom(clazz.annotationType())
                && !WithAzureArmTemplateRepeatable.class.isAssignableFrom(clazz.annotationType())) {
            throw new IllegalArgumentSunstoneException(format("AzureSunstoneDeployer expects %s or %s annotations",
                    WithAzureArmTemplate.class, WithAzureArmTemplateRepeatable.class));
        }
    }
}
