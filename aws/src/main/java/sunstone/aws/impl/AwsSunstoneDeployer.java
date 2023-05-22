package sunstone.aws.impl;


import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.aws.annotation.WithAwsCfTemplateRepeatable;
import sunstone.core.AbstractSunstoneCloudDeployer;
import sunstone.core.SunstoneConfig;
import sunstone.core.SunstoneExtension;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import static java.lang.String.format;

/**
 * Purpose: handles creating resources on AWS.
 * <p>
 * Used by {@link SunstoneExtension} which delegate handling TestClass annotations such as {@link WithAwsCfTemplate}.
 * Lambda function to undeploy resources is also registered for the AfterAllCallback phase.
 * <p>
 * The class works with {@link AwsCloudFormationCloudDeploymentManager}
 * which handles deploy operation to particular cloud vendor.
 */
public class AwsSunstoneDeployer extends AbstractSunstoneCloudDeployer {

    private Annotation annotation;

    AwsSunstoneDeployer(Annotation annotation) {
        this.annotation = annotation;
    }

    @Override
    public void deployAndRegisterForUndeploy(ExtensionContext ctx) throws SunstoneException {
        verify(annotation);
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);

        if (WithAwsCfTemplate.class.isAssignableFrom(annotation.annotationType())) {
            deployCfTemplate((WithAwsCfTemplate) annotation, store);
        } else if (WithAwsCfTemplateRepeatable.class.isAssignableFrom(annotation.annotationType())) {
            for (WithAwsCfTemplate withAwsCfTemplate : ((WithAwsCfTemplateRepeatable) annotation).value()) {
                deployCfTemplate(withAwsCfTemplate, store);
            }
        }
    }

    private void deployCfTemplate(WithAwsCfTemplate awsTemplateDefinition, AwsSunstoneStore store) throws
        SunstoneException {
        AwsCloudFormationCloudDeploymentManager deploymentManager = store.getAwsCfDemploymentManagerOrCreate();

        try {
            String content = getResourceContent(awsTemplateDefinition.template());
            Map<String, String> parameters = getParameters(awsTemplateDefinition.parameters());
            String region = SunstoneConfig.resolveExpressionToString(awsTemplateDefinition.region());
            if (region == null) {
                throw new IllegalArgumentSunstoneException("Region for AWS template is not defined. It must be specified either "
                        + "in the annotation or as Sunstone Config property.");
            }
            if (!awsTemplateDefinition.perSuite() || !store.suiteLevelDeploymentExists(awsTemplateDefinition)) {
                CloudFormationClient cfClient = store.getAwsCfClientOrCreate(region);
                String stack = deploymentManager.deployAndRegister(cfClient, content, parameters);
                if (awsTemplateDefinition.perSuite()) {
                    store.addSuiteLevelClosable(() -> deploymentManager.undeploy(stack));
                    store.addSuiteLevelDeployment(annotation);
                } else {
                    store.addClosable(() -> deploymentManager.undeploy(stack));
                }
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
    }

    private void verify(Annotation clazz) throws IllegalArgumentSunstoneException {
        if (!AwsUtils.propertiesForAwsClientArePresent()) {
            throw new RuntimeException("Missing credentials for AWS.");
        }
        if (!WithAwsCfTemplate.class.isAssignableFrom(clazz.annotationType())
                && !WithAwsCfTemplateRepeatable.class.isAssignableFrom(clazz.annotationType())) {
            throw new IllegalArgumentSunstoneException(format("AwsSunstoneDeployer expects %s or %s annotations",
                    WithAwsCfTemplate.class, WithAwsCfTemplateRepeatable.class));
        }
    }
}
