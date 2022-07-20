package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import sunstone.api.SunstoneResource;
import sunstone.api.WithAwsCfTemplate;
import lombok.extern.slf4j.Slf4j;
import sunstone.api.WithAzureArmTemplate;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class SunstoneExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneExtension");
    private static final String DEPLOYMENT_REGISTERS = "deploymentRegisters";
    private static final String CLOSABLES = "closables";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        log.debug("Sunstone extension has started");
        SunstoneExtensionStoreHelper.setupStore(extensionContext);

        if (extensionContext.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0) {
            awsCloudFormationTemplates(extensionContext);
        }
        if (extensionContext.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0) {
            azureArmTemplates(extensionContext);
        }
    }

    private void injectResources(Object o, ExtensionContext context) throws IllegalAccessException {
        log.debug("injectResources");
        List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(o.getClass(), SunstoneResource.class);
        for (Field field : annotatedFields) {
            SunstoneResource annotation = field.getAnnotation(SunstoneResource.class);
            String name = annotation.named();
            SunstoneResourceInjectHelper.injectAndRegisterResource(field, name, context);
        }
    }

    private void awsCloudFormationTemplates(ExtensionContext context) throws Exception {
        AwsCloudFormationDeploymentManager cfDeploymentMngr = new AwsCloudFormationDeploymentManager();
        SunstoneExtensionStoreHelper.setAwsCfDemploymentManager(context, cfDeploymentMngr);

        CloudFormationClient cfClient = AwsClientFactory.getCloudFormationClient();
        SunstoneExtensionStoreHelper.setAwsCfClient(context, cfClient);
        SunstoneExtensionStoreHelper.closables(context).add(cfClient);

        Ec2Client ec2Client = AwsClientFactory.getEC2Client();
        SunstoneExtensionStoreHelper.setAwsEC2Client(context, ec2Client);
        SunstoneExtensionStoreHelper.closables(context).add(ec2Client);

        SunstoneProvisioningAnnotationHelper.handleAwsCloudFormationAnnotations(context);
        SunstoneExtensionStoreHelper.deploymentRegisters(context).add(cfDeploymentMngr);
        SunstoneExtensionStoreHelper.closables(context).add(cfDeploymentMngr);
    }

    private void azureArmTemplates(ExtensionContext context) throws Exception {
        AzureArmTemplateDeploymentManager azDeploymentMngr = new AzureArmTemplateDeploymentManager();
        SunstoneExtensionStoreHelper.setAzureArmTemplateDeploymentManager(context, azDeploymentMngr);
        SunstoneExtensionStoreHelper.setAzureArmClient(context, AzureClientFactory.getResourceManager());
        SunstoneProvisioningAnnotationHelper.handleAzureArmTemplateAnnotations(context);
        SunstoneExtensionStoreHelper.deploymentRegisters(context).add(azDeploymentMngr);
        SunstoneExtensionStoreHelper.closables(context).add(azDeploymentMngr);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);

        HashSet<DeploymentRegistry> deploymentRegistries = store.get(DEPLOYMENT_REGISTERS, HashSet.class);
        deploymentRegistries.forEach(DeploymentRegistry::undeplyAll);

        HashSet<AutoCloseable> closables = store.get(CLOSABLES, HashSet.class);
        closables.forEach(autoCloseable -> {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        injectResources(testInstance, context);
    }
}
