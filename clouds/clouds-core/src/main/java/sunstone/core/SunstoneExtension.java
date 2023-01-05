package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.spi.SunstoneCloudDeployerProvider;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.ServiceLoader;

import static sunstone.core.SunstoneStore.get;

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
        SunstoneStore store = get(ctx);

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0) {
            handleAzureArmTemplateAnnotations(ctx);
        }

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0) {
            handleAwsCloudFormationAnnotations(ctx);
        }
    }

    static void handleAwsCloudFormationAnnotations(ExtensionContext ctx) {
        Optional<SunstoneCloudDeployer> deployer = getDeployer(WithAwsCfTemplate.class);
        deployer.orElseThrow(() -> new RuntimeException("Unable to load a service via SPI that handles " + WithAwsCfTemplate.class.getName() + " annotation."));
        for (WithAwsCfTemplate withAwsCfTemplate : ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class)) {
            deployer.get().deploy(withAwsCfTemplate, ctx);
        }
    }

    protected static void handleAzureArmTemplateAnnotations(ExtensionContext ctx) {
        Optional<SunstoneCloudDeployer> deployer = getDeployer(WithAzureArmTemplate.class);
        deployer.orElseThrow(() -> new RuntimeException("Unable to load a service via SPI that handles " + WithAzureArmTemplate.class.getName() + " annotation."));
        for (WithAzureArmTemplate withAzureArmTemplate : ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class)) {
            deployer.get().deploy(withAzureArmTemplate, ctx);
        }
    }


    static <A extends Annotation> Optional<SunstoneCloudDeployer> getDeployer(Class<A> annotation) {
        ServiceLoader<SunstoneCloudDeployerProvider> loader = ServiceLoader.load(SunstoneCloudDeployerProvider.class);
        for (SunstoneCloudDeployerProvider sunstoneCloudDeployerProvider : loader) {
            Optional<SunstoneCloudDeployer> deployer = sunstoneCloudDeployerProvider.create(annotation);
            if (deployer.isPresent()) {
                return deployer;
            }
        }
        return Optional.empty();
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        var ref = new Object() {
            Exception e = null;
        };
        get(ctx).getClosablesOrCreate().forEach(closeable -> {
            try {
                closeable.close();
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
