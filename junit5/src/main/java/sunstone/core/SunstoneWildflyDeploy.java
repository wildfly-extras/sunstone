package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.slf4j.Logger;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import sunstone.api.SunstoneResourceHint;
import sunstone.api.WildFlyDeployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

import static sunstone.api.SunstoneResourceHint.AWS_EC2_INSTANCE;
import static sunstone.api.SunstoneResourceHint.AZ_VM_INSTANCE;
import static sunstone.api.SunstoneResourceHint.AZ_WEB_APP;
import static sunstone.api.SunstoneResourceHint.JCLOUDS_NODE;
import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Purpose: handles archive deployment operation to WildFly server.
 *
 * Used by {@link SunstoneExtension}. The class is also responsible for registering undeploy operation if possible.
 */
class SunstoneWildflyDeploy {
    static Logger LOGGER = SunstoneJUnit5Logger.DEFAULT;

    /**
     * Find annotated method, get the deployment and deploy using ManagementClient to EC2 and Azure VMs or
     * Azure SDK to Azure Web APP (app service)
     */
    static void performDeploymentOperation(ExtensionContext ctx) throws Exception {
        SunstoneStore store = StoreWrapper(ctx);
        List<Method> annotatedMethods = AnnotationSupport.findAnnotatedMethods(ctx.getRequiredTestClass(), WildFlyDeployment.class, HierarchyTraversalMode.TOP_DOWN);
        for (Method method : annotatedMethods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new RuntimeException("Deployment method must be static");
            }
            if (method.getParameterCount() != 0) {
                throw new RuntimeException("Deployment method must have 0 parameters");
            }
            WildFlyDeployment annotation = method.getAnnotation(WildFlyDeployment.class);
            String deploymentName = ObjectProperties.replaceSystemProperties(annotation.name());
            String resourceName = ObjectProperties.replaceSystemProperties(annotation.to());
            SunstoneResourceHint resourceType = annotation.hint();

            Object invoke = method.invoke(null);
            if (invoke == null) {
                throw new RuntimeException("return object is null");
            }
            InputStream is;
            if (invoke instanceof Archive) {
                is = ((Archive) invoke).as(ZipExporter.class).exportAsInputStream();
            } else if (invoke instanceof File) {
                is = new FileInputStream((File) invoke);
            } else if (invoke instanceof Path) {
                is = new FileInputStream(((Path) invoke).toFile());
            } else if (invoke instanceof InputStream) {
                is = (InputStream) invoke;
            } else {
                throw new RuntimeException("Unknown type for " + method.getName());
            }

            if (resourceType == AWS_EC2_INSTANCE || resourceType == AZ_VM_INSTANCE || resourceType == JCLOUDS_NODE) {
                OnlineManagementClient client = CreaperUtils.getManagementClient(ctx, resourceName, resourceType);
                try {
                    LOGGER.debug("Deploying {} artifact to {} {}", deploymentName, resourceName, resourceType);
                    client.apply(new Deploy.Builder(is, deploymentName, true).build());
                } catch (CommandFailedException e) {
                    throw new RuntimeException(e);
                }
                store.closables().push(() -> {
                    client.apply(new Undeploy.Builder(deploymentName).build());
                    client.close();
                });
            } else if (resourceType == AZ_WEB_APP) {
                Path tempFile = Files.createTempFile("sunstone-deployment-", ".war");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                AzureResourceManager azureArmClient = store.getAzureArmClient();
                Set<String> usedRG = store.getAzureArmTemplateDeploymentManager().getUsedRG();
                WebApp azureWebApp = AzureUtils.findAzureWebApp(azureArmClient, resourceName, usedRG);
                Exception ex = null;
                for (int i = 0; i < 5; i++) {
                    try{
                        LOGGER.debug("Deploying artifact to {} {}", resourceName, resourceType);
                        azureWebApp.warDeploy(tempFile.toFile());
                        ex = null;
                        break;
                    } catch (Exception e) {
                        LOGGER.debug("WebApp::warDeploy try no. {} failed", i);
                        e.printStackTrace();
                        ex = e;
                        // try another time
                        Thread.sleep(10*1000);
                    }
                }
                if (ex != null) {
                    throw ex;
                }
                azureWebApp.restart();
                AzureUtils.waitForWebApp(azureWebApp);
            }
        }
    }
}
