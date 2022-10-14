package sunstone.core;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.api.inject.Hostname;
import sunstone.api.SunstoneResource;
import sunstone.api.SunstoneResourceHint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static sunstone.core.SunstoneStore.StoreWrapper;

/**
 * Purpose: handles resources injection
 *
 * Used by {@link SunstoneExtension} which delegates injecting resources and registering them so that they
 * can be closed, whatever that means - usually just closing clients.
 */
class SunstoneInject {
    static Logger LOGGER = SunstoneJUnit5Logger.DEFAULT;
    static void injectInstanceResources(ExtensionContext ctx, Object instance) throws Exception {
        List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(instance.getClass(), SunstoneResource.class);
        for (Field field : annotatedFields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                SunstoneResource annotation = field.getAnnotation(SunstoneResource.class);
                String name = annotation.of();
                SunstoneResourceHint hint = annotation.hint();
                injectAndRegisterResource(field, instance, name, hint, ctx);
            }
        }
    }

    static void injectStaticResources(ExtensionContext ctx, Class clazz) throws Exception {
        List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(clazz, SunstoneResource.class);
        for (Field field : annotatedFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                SunstoneResource annotation = field.getAnnotation(SunstoneResource.class);
                String name = annotation.of();
                SunstoneResourceHint hint = annotation.hint();
                injectAndRegisterResource(field, null, name, hint, ctx);
            }
        }
    }

    static void injectAndRegisterResource(Field field, Object instance, String named, SunstoneResourceHint hint, ExtensionContext ctx) throws Exception {
        Object injected = null;
        // inject EC2client
        if (Ec2Client.class.isAssignableFrom(field.getType())) {
            Ec2Client ec2Client = AwsUtils.getEC2Client();
            StoreWrapper(ctx).closables().push(ec2Client);
            LOGGER.debug("Injecting {} into {}", ec2Client.getClass().getName(), field.getName());
            injected = ec2Client;
        }
        // inject S3Client
        else if (S3Client.class.isAssignableFrom(field.getType())) {
            S3Client s3Client = AwsUtils.getS3Client();
            StoreWrapper(ctx).closables().push(s3Client);
            LOGGER.debug("Injecting {} into {}", s3Client.getClass().getName(), field.getName());
            injected = s3Client;
        }
        // inject PublicIPAddress
        else if (Hostname.class.isAssignableFrom(field.getType())) {
            switch (hint) {
                case AZ_VM_INSTANCE:
                    VirtualMachine vm = AzureUtils.findAzureVM(StoreWrapper(ctx).getAzureArmClient(), named, StoreWrapper(ctx).getAzureArmTemplateDeploymentManager().getUsedRG());
                    if (vm != null) {
                        LOGGER.debug("Injecting {} ()->\"{}\" of Azure VM {} into {}", Hostname.class.getName(), vm.getPrimaryPublicIPAddress().ipAddress(), named, field.getName());
                        injected = (Hostname) vm.getPrimaryPublicIPAddress()::ipAddress;
                    } else {
                        LOGGER.error("Unable to find {} Azure VM", named);
                    }
                    break;
                case AWS_EC2_INSTANCE:
                    Instance ec2Instance = AwsUtils.findEc2InstanceByNameTag(StoreWrapper(ctx).getAwsEC2Client(), named);
                    if (ec2Instance != null) {
                        LOGGER.debug("Injecting {} ()->\"{}\" of EC2 {} into {}", Hostname.class.getName(), ec2Instance.publicIpAddress(), named, field.getName());
                        injected = (Hostname) ec2Instance::publicIpAddress;
                    } else {
                        LOGGER.error("Unable to find {} AWS EC2 instance", named);
                    }
                    break;
                case AZ_WEB_APP:
                    WebApp app = AzureUtils.findAzureWebApp(StoreWrapper(ctx).getAzureArmClient(), named, StoreWrapper(ctx).getAzureArmTemplateDeploymentManager().getUsedRG());
                    if (app != null) {
                        LOGGER.debug("Injecting {} ()->\"{}\" of Azure Web APP {} into {}", Hostname.class.getName(), app.defaultHostname(), named, field.getName());
                        injected = (Hostname) app::defaultHostname;
                    } else {
                        LOGGER.error("Unable to find {} Azure Web APP", named);
                    }
                    break;
                case JCLOUDS_NODE:
                    // todo
                    break;
            }
        }
        // inject Azure Resource manager
        else if (AzureResourceManager.class.isAssignableFrom(field.getType())) {
            AzureResourceManager arm = AzureUtils.getResourceManager();
            LOGGER.debug("Injecting {} into {}", arm.getClass().getName(), field.getName());
            injected = arm;
        }
        // inject OnlineManagementClient
        else if (OnlineManagementClient.class.isAssignableFrom(field.getType())) {
            OnlineManagementClient managementClient = CreaperUtils.getManagementClient(ctx, named, hint);
            LOGGER.debug("Injecting {} into {}", managementClient.getClass().getName(), field.getName());
            StoreWrapper(ctx).closables().push(managementClient);
            injected = managementClient;
        } else {
            throw new RuntimeException("Unable to determine what should be injected into field of type: " + field.getType().getSimpleName());
        }
        field.setAccessible(true);
        field.set(instance, injected);
    }
}
