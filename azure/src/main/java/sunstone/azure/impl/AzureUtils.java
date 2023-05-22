package sunstone.azure.impl;


import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import sunstone.core.SunstoneConfig;

import java.util.Optional;

public class AzureUtils {
    static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(SunstoneConfig.getString(AzureConfig.SUBSCRIPTION_ID));
    }
    static PostgreSqlManager getPgsqlManager() {
        return PostgreSqlManager
                .authenticate(getCredentials(), new AzureProfile(SunstoneConfig.getString(AzureConfig.TENANT_ID), SunstoneConfig.getString(AzureConfig.SUBSCRIPTION_ID), AzureEnvironment.AZURE));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(SunstoneConfig.getString(AzureConfig.TENANT_ID))
                .clientId(SunstoneConfig.getString(AzureConfig.APPLICATION_ID))
                .clientSecret(SunstoneConfig.getString(AzureConfig.PASSWORD))
                .build();
    }

    static boolean propertiesForArmClientArePresent() {
        return SunstoneConfig.unwrap().isPropertyPresent(AzureConfig.SUBSCRIPTION_ID)
                && SunstoneConfig.unwrap().isPropertyPresent(AzureConfig.TENANT_ID)
                && SunstoneConfig.unwrap().isPropertyPresent(AzureConfig.APPLICATION_ID)
                && SunstoneConfig.unwrap().isPropertyPresent(AzureConfig.PASSWORD);
    }

    static Optional<VirtualMachine> findAzureVM(AzureResourceManager arm, String name, String resourceGroup) {
        return arm.virtualMachines().listByResourceGroup(resourceGroup).stream()
                .filter(vm -> vm.name().equals(name))
                .findAny();
    }

    static Optional<WebApp> findAzureWebApp(AzureResourceManager arm, String name, String resourceGroup) {
        try {
            return Optional.ofNullable(arm.webApps().getByResourceGroup(resourceGroup, name));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    static Optional<AppServicePlan> findAzureAppServicePlan(AzureResourceManager arm, String name, String resourceGroup) {
        try {
            return Optional.ofNullable(arm.appServicePlans().getByResourceGroup(resourceGroup, name));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    static Optional<Server> findAzurePgSqlServer(PostgreSqlManager pgsqlManager, String name, String resourceGroup) {
        try {
            return Optional.ofNullable(pgsqlManager.servers().getByResourceGroup(resourceGroup, name));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
