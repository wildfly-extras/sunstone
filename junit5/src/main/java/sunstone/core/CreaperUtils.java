package sunstone.core;


import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.api.SunstoneResourceHint;

import java.io.IOException;

import static sunstone.core.SunstoneStore.StoreWrapper;

public class CreaperUtils {
    static Logger LOGGER = SunstoneJUnit5Logger.DEFAULT;
    static OnlineManagementClient getManagementClient(ExtensionContext ctx, String resourceName, SunstoneResourceHint hint) throws IOException {
        int port = Integer.parseInt(new ObjectProperties(ObjectType.JUNIT5, null).getProperty(JUnit5Config.JUnit5.WildFly.MNGMT_PORT));
        switch (hint) {
            case AZ_VM_INSTANCE:
                VirtualMachine vm = AzureUtils.findAzureVM(StoreWrapper(ctx).getAzureArmClient(), resourceName, StoreWrapper(ctx).getAzureArmTemplateDeploymentManager().getUsedRG());
                if (vm != null) {
                    return createManagementClient(vm.getPrimaryPublicIPAddress().ipAddress(), port);
                }
                break;
            case AWS_EC2_INSTANCE:
                Instance ec2Instance = AwsUtils.findEc2InstanceByNameTag(StoreWrapper(ctx).getAwsEC2Client(), resourceName);
                if (ec2Instance != null) {
                    return createManagementClient(ec2Instance.publicIpAddress(), port);
                }
                break;
            case JCLOUDS_NODE:
                // todo
                break;
        }
        throw new RuntimeException("Unable to create management client!");
    }

    static OnlineManagementClient createManagementClient(String hostname, int port) throws IOException {
        ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);
        String user = objectProperties.getProperty(JUnit5Config.JUnit5.WildFly.MNGMT_USERNAME);
        String pass = objectProperties.getProperty(JUnit5Config.JUnit5.WildFly.MNGMT_PASSWORD);
        int timeout = Integer.parseInt(objectProperties.getProperty(JUnit5Config.JUnit5.WildFly.MNGMT_CONNECTION_TIMEOUT));
        LOGGER.debug("Creating management client {}:{} using credentials {}:{} with timeout {}", hostname, port, user, pass, timeout);
        OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.standalone()
                .hostAndPort(hostname, port)
                .auth(user, pass)
                .connectionTimeout(timeout)
                .bootTimeout(timeout);
        return ManagementClient.online(clientOptions.build());
    }
}
