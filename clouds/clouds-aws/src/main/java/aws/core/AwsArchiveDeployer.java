package aws.core;


import aws.core.AwsIdentifiableSunstoneResource.Identification;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.InputStream;
import java.lang.annotation.Annotation;


/**
 * Purpose: handle deploy operation to WildFly.
 *
 * Heavily uses {@link AwsIdentifiableSunstoneResource} to determine the destination of deploy operation
 *
 * To retrieve Aws cloud resources, the class relies on {@link AwsIdentifiableSunstoneResource#get(Annotation, AwsSunstoneStore, Class)}.
 *
 * Undeploy operations are registered in the extension store so that they are closed once the store is closed
 */
public class AwsArchiveDeployer implements SunstoneArchiveDeployer {
    static void deployToEc2Instance(String deploymentName, Identification resourceIdentification, InputStream is, AwsSunstoneStore store) throws SunstoneException {
        try {
            OnlineManagementClient client = AwsIdentifiableSunstoneResourceUtils.resolveOnlineManagementClient(resourceIdentification, store);
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
        AwsSunstoneStore store = AwsSunstoneStore.get(ctx);
        Identification identification = new Identification(targetAnnotation);

        if (!identification.type.deployToWildFlySupported()) {
            throw new UnsupportedSunstoneOperationException("todo");
        }

        switch (identification.type) {
            case EC2_INSTANCE:
                if (deploymentName.isEmpty()) {
                    throw new IllegalArgumentSunstoneException("Deployment name can not be empty for AWS EC2 instance.");
                }
                deployToEc2Instance(deploymentName, identification, deployment, store);
                break;
            default:
                throw new UnsupportedSunstoneOperationException("Deployment operation is not supported for " + identification.type);
        }
    }
}
