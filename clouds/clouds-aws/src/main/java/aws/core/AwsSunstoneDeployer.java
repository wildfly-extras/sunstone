package aws.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import sunstone.api.WithAwsCfTemplate;
import sunstone.core.AbstractSunstoneCloudDeployer;
import sunstone.core.SunstoneExtension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
    @Override
    public void deploy(Annotation annotation, ExtensionContext ctx) {
        verifyClass(annotation);
        WithAwsCfTemplate awsTemplateDefinition = (WithAwsCfTemplate) annotation;

        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);
        AwsCloudFormationCloudDeploymentManager deploymentManager = store.getAwsCfDemploymentManagerOrCreate();

        try {
            String content = getResourceContent(awsTemplateDefinition.template());
            Map<String, String> parameters = getParameters(awsTemplateDefinition.parameters());
            String region = resolveOrGetFromSunstoneProperties(awsTemplateDefinition.region(), AwsConfig.REGION);
            if (region == null) {
                throw new IllegalArgumentException("Region for AWS template is not defined. It must be specified either"
                        + "in the annotation or in sunstone.properties file");
            }
            String md5sum = md5sum(content);

            if (!awsTemplateDefinition.perSuite() || !store.suiteLevelDeploymentExists(md5sum)) {
                CloudFormationClient cfClient = store.getAwsCfClientOrCreate(region);
                String stack = deploymentManager.deployAndRegister(cfClient, content, parameters);
                if (awsTemplateDefinition.perSuite()) {
                    store.addSuiteLevelClosable(() -> deploymentManager.undeploy(stack));
                    store.addSuiteLevelDeployment(md5sum);
                } else {
                    store.addClosable(() -> deploymentManager.undeploy(stack));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyClass(Annotation clazz) {
        if (!AwsUtils.propertiesForAwsClientArePresent()) {
            throw new RuntimeException("Missing credentials for AWS.");
        }

    }
}
