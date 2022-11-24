package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;

import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Extension handles {@link WithAzureArmTemplate} annotation, serves and orchestrate/delegate all the work,
 * initialize resources, ...
 * <p>
 * Uses {@link AutoCloseable} LIFO queue (a.k.a. Stack) for registering resources that needs to be cleaned/closed -
 * closing clients, cleaning Cloud resources ... Class that creates
 * a resource that needs to be cleaned/closed is also responsible for registering it.
 */
public class SunstoneExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        store.initClosables();

        // cache Azure ARM
        if (AzureUtils.propertiesForArmClientArePresent()) {
        }

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0) {
            if (!AzureUtils.propertiesForArmClientArePresent()) {
                throw new RuntimeException("Missing credentials for Azure.");
            }
            SunstoneCloudDeploy.handleAzureArmTemplateAnnotations(ctx);
        }

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0) {
            if (!AwsUtils.propertiesForAwsClientArePresent()) {
                throw new RuntimeException("Missing credentials for AWS.");
            }
            SunstoneCloudDeploy.handleAwsCloudFormationAnnotations(ctx);
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
}
