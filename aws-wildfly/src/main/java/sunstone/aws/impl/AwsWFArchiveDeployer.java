package sunstone.aws.impl;


import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.DomainMode;
import sunstone.annotation.WildFly;
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
import java.nio.file.Path;

import static java.lang.String.format;
import static sunstone.aws.impl.AwsWFIdentifiableSunstoneResource.*;
import static sunstone.core.CreaperUtils.setDomainServers;

/**
 * Purpose: handle deploy operation to WildFly.
 *
 * Heavily uses {@link AwsWFIdentifiableSunstoneResource} to determine the destination of deploy operation
 *
 * To retrieve Aws cloud resources, the class relies on {@link AwsWFIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)}.
 *
 * Undeploy operations are registered in the extension store so that they are closed once the store is closed
 */
public class AwsWFArchiveDeployer implements SunstoneArchiveDeployer {

    private final Identification identification;
    private WildFly wildFly;

    AwsWFArchiveDeployer(Identification identification, WildFly wildFly) {
        this.identification = identification;
        this.wildFly = wildFly;
    }

    static void undeployFromEc2Instance(String deploymentName, Identification resourceIdentification, WildFly wildFly, AwsSunstoneStore store) throws SunstoneException {
        try (OnlineManagementClient client = AwsWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, wildFly, store)){
            client.apply(new Undeploy.Builder(deploymentName).build());
        } catch (CommandFailedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void deployToEc2Instance(String deploymentName, Identification resourceIdentification, WildFly wildFly, InputStream is, AwsSunstoneStore store) throws SunstoneException {
        Deploy.Builder builder = new Deploy.Builder(is, deploymentName, true);
        DomainMode domainMode = wildFly.domain();
        //no further configuration needed for standalone mode,
        //in domain mode, we need to specify server groups
        if (domainMode != null) {
            setDomainServers(builder,domainMode);
        }
        try (OnlineManagementClient client = AwsWFIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, wildFly, store)) {
            client.apply(builder.build());
        } catch (CommandFailedException | IOException e) {
            throw new SunstoneException(e);
        }
    }

    @Override
    public void deploy(String deploymentName, Object object, ExtensionContext ctx) throws SunstoneException {
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);
        if (!identification.type.deployToWildFlySupported()) {
            throw new UnsupportedSunstoneOperationException(format("Unsupported target %s for deploy operation", identification.type));
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
        try {
            switch (identification.type) {
                case EC2_INSTANCE:
                    if (StringUtils.isBlank(deploymentName)) {
                        throw new IllegalArgumentSunstoneException("Deployment name can not be empty for AWS EC2 instance.");
                    }
                    deployToEc2Instance(deploymentName, identification, wildFly, is, store);
                    break;
                default:
                    throw new UnsupportedSunstoneOperationException("Deployment operation is not supported for " + identification.type);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new SunstoneException(e);
                }
            }
        }
    }

    @Override
    public void undeploy(String deploymentName, ExtensionContext ctx) throws SunstoneException {
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);
        switch (identification.type) {
            case EC2_INSTANCE:
                if (StringUtils.isBlank(deploymentName)) {
                    throw new IllegalArgumentSunstoneException("Deployment name can not be empty for Azure virtual machine.");
                }
                undeployFromEc2Instance(deploymentName, identification, wildFly, store);
                break;
            default:
                throw new UnsupportedSunstoneOperationException(format("Unknown resource type for undeploy operation from %s", identification.type));
        }
    }
}
