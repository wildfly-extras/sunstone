package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;

import java.lang.reflect.Field;

public class SunstoneResourceInjectHelper {
    static void injectAndRegisterResource(Field field, String named, ExtensionContext context) throws IllegalAccessException {
        Object injected = null;
        if (Ec2Client.class.isAssignableFrom(field.getType())) {
            Ec2Client ec2Client = AwsClientFactory.getEC2Client();
            SunstoneExtensionStoreHelper.closables(context).add(ec2Client);
            injected = ec2Client;
        } else if (AzureResourceManager.class.isAssignableFrom(field.getType())) {
            injected = AzureClientFactory.getResourceManager();
        } else if (OnlineManagementClient.class.isAssignableFrom(field.getType())) {
            if (fromAzureArmTemplate(context)) {
                VirtualMachine vm = findAzureVM(context, named);
                if (vm != null) {
                    injected = OnlineOptions.standalone().hostAndPort(vm.getPrimaryPublicIPAddress().ipAddress(), 9990);
                }
            } else if (fromAwsCFTemplate(context)) {
                Instance ec2Instance = findEC2Instance(context, named);
                if (ec2Instance != null) {
                    injected = OnlineOptions.standalone().hostAndPort(ec2Instance.publicIpAddress(), 9990);
                }
            } else if (fromJClouds(context)) {
                // find node
            }
        } else {
            throw new RuntimeException("Unable to determine what should be injected into field of type: " + field.getType().getSimpleName());
        }

        field.setAccessible(true);
        field.set(field, injected);
    }

    private static VirtualMachine findAzureVM(ExtensionContext context, String name) {
        AzureArmTemplateDeploymentManager azManager = SunstoneExtensionStoreHelper.getAzureArmTemplateDeploymentManager(context);
        AzureResourceManager arm = SunstoneExtensionStoreHelper.getAzureArmClient(context);
        for (String rg : azManager.getUsedRG()) {
            VirtualMachine vm = arm.virtualMachines().getByResourceGroup(rg, name);
            if (vm != null) {
                return vm;
            }
        }
        return null;
    }

    private static Instance findEC2Instance(ExtensionContext context, String name) {

        try {
            String nextToken = null;
            Filter runningInstancesFilter = Filter.builder()
                    .name("instance-state-name")
                    .values("running")
                    .build();
            Filter tagFilter = Filter.builder()
                    .name("tag:Name")
                    .values(name)
                    .build();

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .filters(runningInstancesFilter, tagFilter)
                    .build();

            DescribeInstancesResponse response = SunstoneExtensionStoreHelper.getAwsEC2Client(context)
                    .describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    for (Tag tag : instance.tags()) {
                        if (tag.key().equals("Name") && tag.value().equals(name)) {
                            return instance;
                        }
                    }
                }
            }
        } catch (Ec2Exception e) {
            // todo
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }

    private static boolean fromJClouds(ExtensionContext context) {
        // todo
        return false;
    }

    private static boolean fromAwsCFTemplate(ExtensionContext context) {
        return context.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0;
    }

    private static boolean fromAzureArmTemplate(ExtensionContext context) {
        return context.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0;
    }
}
