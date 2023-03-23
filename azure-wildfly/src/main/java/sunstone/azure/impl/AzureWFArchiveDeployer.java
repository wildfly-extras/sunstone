package sunstone.azure.impl;


import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.platform.commons.util.StringUtils;
import sunstone.annotation.WildFly;
import sunstone.azure.impl.AzureWFIdentifiableSunstoneResource.Identification;
import com.azure.resourcemanager.appservice.models.DeployType;
import com.azure.resourcemanager.appservice.models.PublishingProfile;
import com.azure.resourcemanager.appservice.models.WebApp;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


/**
 * Purpose: handle deploy operation to WildFly.
 *
 * Heavily uses {@link AzureWFIdentifiableSunstoneResource} to determine the destination of deploy operation
 *
 * To retrieve Azure cloud resources, the class relies on {@link AzureWFIdentifiableSunstoneResource#get(Annotation, AzureSunstoneStore, Class)}.
 *
 * Undeploy operations are registered in the extension store so that they are closed once the store is closed
 */
public class AzureWFArchiveDeployer implements SunstoneArchiveDeployer {

    private final Identification identification;
    private WildFly wildFly;

    static void deployToWebApp(Identification resourceIdentification, InputStream is, AzureSunstoneStore store) throws Exception {
        Path tempFile = Files.createTempFile("sunstone-war-deployment-", ".war");
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        WebApp azureWebApp = resourceIdentification.get(store, WebApp.class);
        azureWebApp.deployAsync(DeployType.WAR, tempFile.toFile()).block();
        azureWebApp.restartAsync().block();
        AzureWFUtils.waitForWebAppDeployment(azureWebApp);
    }

    static void undeployFromWebApp(Identification resourceIdentification, AzureSunstoneStore store) throws SunstoneException {
        WebApp webApp = resourceIdentification.get(store, WebApp.class);
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

            webApp.restartAsync().block();
            AzureWFUtils.waitForWebAppCleanState(webApp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void deployToVmInstance(String deploymentName, Identification resourceIdentification, WildFly wildFly, InputStream is, AzureSunstoneStore store) throws SunstoneException {
        try (OnlineManagementClient client = AzureWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, wildFly, store)){
            client.apply(new Deploy.Builder(is, deploymentName, false).build());
        } catch (CommandFailedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void undeployFromVmInstance(String deploymentName, Identification resourceIdentification, WildFly wildFly, AzureSunstoneStore store) throws SunstoneException {
        try (OnlineManagementClient client = AzureWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, wildFly, store)){
            client.apply(new Undeploy.Builder(deploymentName).build());
        } catch (CommandFailedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    AzureWFArchiveDeployer(Identification identification, WildFly wildFly) {
        this.identification = identification;
        this.wildFly = wildFly;
    }

    @Override
    public void deploy(String deploymentName, Object object, ExtensionContext ctx) throws SunstoneException {
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);

        if (!identification.type.deployToWildFlySupported()) {
            throw new UnsupportedSunstoneOperationException("todo");
        }
        InputStream is;
        try {
            if (object instanceof Archive) {
                is = ((Archive<?>) object).as(ZipExporter.class).exportAsInputStream();
            } else if (object instanceof File) {
                is = new FileInputStream((File) object);
            } else if (object instanceof Path) {
                is = new FileInputStream(((Path) object).toFile());
            } else if (object instanceof InputStream) {
                is = (InputStream) object;
            } else {
                throw new UnsupportedSunstoneOperationException("Unsupported type for deployment operation");
            }
        } catch (FileNotFoundException e) {
            throw new SunstoneException(e);
        }

        switch (identification.type) {
            case VM_INSTANCE:
                if (StringUtils.isBlank(deploymentName)) {
                    throw new IllegalArgumentSunstoneException("Deployment name can not be empty for Azure virtual machine.");
                }
                deployToVmInstance(deploymentName, identification, wildFly, is, store);
                break;
            case WEB_APP:
                try {
                    if (!deploymentName.isEmpty()) {
                        throw new IllegalArgumentSunstoneException("Deployment name must be empty for Azure Web App. It is always ROOT.war and only WAR is supported.");
                    }
                    deployToWebApp(identification, is, store);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new UnsupportedSunstoneOperationException("todo");
        }
    }

    @Override
    public void undeploy(String deploymentName, ExtensionContext ctx) throws SunstoneException {
        AzureSunstoneStore store = AzureSunstoneStore.get(ctx);
        switch (identification.type) {
            case VM_INSTANCE:
                if (StringUtils.isBlank(deploymentName)) {
                    throw new IllegalArgumentSunstoneException("Deployment name can not be empty for Azure virtual machine.");
                }
                undeployFromVmInstance(deploymentName, identification, wildFly, store);
                break;
            case WEB_APP:
                try {
                    if (!deploymentName.isEmpty()) {
                        throw new IllegalArgumentSunstoneException("Deployment name must be empty for Azure Web App. It is always ROOT.war and only WAR is supported.");
                    }
                    undeployFromWebApp(identification, store);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new UnsupportedSunstoneOperationException("todo");
        }
    }
}
