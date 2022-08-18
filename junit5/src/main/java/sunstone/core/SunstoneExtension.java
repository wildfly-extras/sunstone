package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import sunstone.api.AbstractSetupTask;
import sunstone.api.Setup;
import sunstone.api.Sunstone;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;

import java.util.ArrayDeque;

import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Extension handles {@link Sunstone} annotation, serves and orchestrate/delegate all the work,
 * initialize resources, ...
 *
 * Uses {@link AutoCloseable} LIFO queue (a.k.a. Stack) for registering resources that needs to be cleaned/closed -
 * closing clients, cleaning Cloud resources, calling {@link AbstractSetupTask#cleanup()}, ... Class that creates
 * a resource that needs to be cleaned/closed is also responsible for registering it.
 */
public class SunstoneExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        StoreWrapper(ctx).setClosables(new ArrayDeque<>());

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0) {
            CloudFormationClient cfClient = AwsUtils.getCloudFormationClient();
            StoreWrapper(ctx).setAwsCfClient(cfClient);
            StoreWrapper(ctx).closables().push(cfClient);

            Ec2Client ec2Client = AwsUtils.getEC2Client();
            StoreWrapper(ctx).setAwsEC2Client(ec2Client);
            StoreWrapper(ctx).closables().push(ec2Client);

            SunstoneCloudDeploy.handleAwsCloudFormationAnnotations(ctx);
        }
        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0) {
            StoreWrapper(ctx).setAzureArmClient(AzureUtils.getResourceManager());
            SunstoneCloudDeploy.handleAzureArmTemplateAnnotations(ctx);
        }
        if (ctx.getRequiredTestClass().getAnnotationsByType(Setup.class).length > 0) {
            handleSetup(ctx);
        }
        SunstoneInject.injectStaticResources(ctx, ctx.getRequiredTestClass());
        SunstoneWildflyDeploy.performDeploymentOperation(ctx);
    }

    private void handleSetup(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        if (ctx.getRequiredTestClass().getAnnotationsByType(Setup.class).length != 1) {
            throw new RuntimeException();
        }
        Setup setup = ctx.getRequiredTestClass().getAnnotationsByType(Setup.class)[0];
        for (Class<? extends AbstractSetupTask> setupTask : setup.value()) {
            SunstoneInject.injectStaticResources(ctx, setupTask);
            AbstractSetupTask abstractSetupTask = setupTask.getDeclaredConstructor().newInstance();
            SunstoneInject.injectInstanceResources(ctx, abstractSetupTask);
            store.closables().push(abstractSetupTask::cleanup);
            abstractSetupTask.setup();
        }
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        var ref = new Object() {
            Exception e = null;
        };
        StoreWrapper(ctx).closables().forEach(autoCloseable -> {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                if (ref.e == null) {
                    ref.e = e;
                }
            }
        });
        if (ref.e != null) {
            throw ref.e;
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext ctx) throws Exception {
        SunstoneInject.injectInstanceResources(ctx, testInstance);
    }
}
