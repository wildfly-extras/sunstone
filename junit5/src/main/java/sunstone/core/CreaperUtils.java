package sunstone.core;


import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.api.SunstoneResourceHint;

import java.io.IOException;

import static sunstone.core.SunstoneStore.StoreWrapper;

public class CreaperUtils {
    static OnlineManagementClient getManagementClient(ExtensionContext ctx, String resourceName, SunstoneResourceHint hint) throws IOException {
        switch (hint) {
            case AZ_VM_INSTANCE:
                VirtualMachine vm = AzureUtils.findAzureVM(StoreWrapper(ctx).getAzureArmClient(), resourceName, StoreWrapper(ctx).getAzureArmTemplateDeploymentManager().getUsedRG());
                if (vm != null) {
                    return createManagementClient(vm.getPrimaryPublicIPAddress().ipAddress(), 9990);
                }
                break;
            case AWS_EC2_INSTANCE:
                Instance ec2Instance = AwsUtils.findEc2InstanceByNameTag(StoreWrapper(ctx).getAwsEC2Client(), resourceName);
                if (ec2Instance != null) {
                    return createManagementClient(ec2Instance.publicIpAddress(), 9990);
                }
                break;
            case JCLOUDS_NODE:
                // todo
                break;
        }
        throw new RuntimeException("Unable to create management client!");
    }

    static OnlineManagementClient createManagementClient(String hostname, int port) throws IOException {
        OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.standalone()
                .hostAndPort(hostname, port)
                .auth("admin", "pass.1234")
                .connectionTimeout(60000)
                .bootTimeout(60000);

        return ManagementClient.online(clientOptions.build());
    }
}
