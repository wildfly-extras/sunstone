package azure.core;


import azure.core.AzureIdentifiableSunstoneResource.Identification;
import com.azure.resourcemanager.appservice.models.PublishingProfile;
import com.azure.resourcemanager.appservice.models.WebApp;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.core.TimeoutUtils;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * Purpose: handle deploy operation to WildFly.
 *
 * Heavily uses {@link AzureIdentifiableSunstoneResource} to determine the destination of deploy operation
 *
 * To retrieve Azure cloud resources, the class relies on {@link AzureIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)}.
 *
 * Undeploy operations are registered in the extension store so that they are closed once the store is closed
 */
public class AzureArchiveDeployer implements SunstoneArchiveDeployer {

    static void deployToWebApp(Identification resourceIdentification, InputStream is, AzureSunstoneStore store) throws Exception {
        Path tempFile = Files.createTempFile("sunstone-war-deployment-", ".war");
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        WebApp azureWebApp = resourceIdentification.get(store, WebApp.class);

        Exception ex = null;
        for (int i = 0; i < 5; i++) {
            try {
                azureWebApp.warDeploy(tempFile.toFile());
                ex = null;
                break;
            } catch (Exception e) {
                AzureLogger.DEFAULT.debug("WebApp::warDeploy try no. {} failed", i);
                e.printStackTrace();
                ex = e;
                // try another time
                Thread.sleep(10 * 1000);
            }
        }
        if (ex != null) {
            throw ex;
        }
        store.addClosable(() -> undeployFromWebApp(azureWebApp));

        azureWebApp.restart();
        // restart is started after some unknown time (async) :/
        Thread.sleep(TimeoutUtils.adjust(30000));
        AzureUtils.waitForWebAppDeployment(azureWebApp);
    }

    static void undeployFromWebApp(WebApp webApp) {
        PublishingProfile profile = webApp.getPublishingProfile();
        FTPClient ftpClient = new FTPClient();
        String[] ftpUrlSegments = profile.ftpUrl().split("/", 2);
        String server = ftpUrlSegments[0];
        try {
            ftpClient.connect(server);
            ftpClient.enterLocalPassiveMode();
            ftpClient.login(profile.ftpUsername(), profile.ftpPassword());
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            FtpUtils.cleanDirectory(ftpClient, "/site/wwwroot/");

            ftpClient.disconnect();

            webApp.restart();
            // restart is started after some unknown time (async) :/
            Thread.sleep(TimeoutUtils.adjust(30000));
            AzureUtils.waitForWebAppCleanState(webApp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void deployToVmInstance(String deploymentName, Identification resourceIdentification, InputStream is, AzureSunstoneStore store) throws SunstoneException {
        try {
            OnlineManagementClient client = AzureIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, store);
            client.apply(new Deploy.Builder(is, deploymentName, true).build());
            store.addClosable((AutoCloseable) () -> {
                client.apply(new Undeploy.Builder(deploymentName).build());
                client.close();
            });
        } catch (CommandFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deployAndRegisterUndeploy(String deploymentName, Annotation targetAnnotation, InputStream deployment, ExtensionContext ctx) throws SunstoneException {
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);
        Identification identification = new Identification(targetAnnotation);

        if (!identification.type.deployToWildFlySupported()) {
            throw new UnsupportedSunstoneOperationException("todo");
        }

        switch (identification.type) {
            case VM_INSTANCE:
                if (deploymentName.isEmpty()) {
                    throw new IllegalArgumentSunstoneException("Deployment name can not be empty for Azure virtual machine.");
                }
                deployToVmInstance(deploymentName, identification, deployment, store);
                break;
            case WEB_APP:
                try {
                    if (!deploymentName.isEmpty()) {
                        AzureLogger.DEFAULT.warn("Deployment name for Azure Web app is not empty. The name will be ignored and the archive will be deployed as ROOT.war");
                    }
                    deployToWebApp(identification, deployment, store);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new UnsupportedSunstoneOperationException("todo");
        }
    }
}
