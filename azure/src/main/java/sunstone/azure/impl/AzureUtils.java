package sunstone.azure.impl;


import com.azure.core.credential.TokenCredential;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.monitor.models.EventData;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import org.slf4j.Logger;
import sunstone.core.SunstoneConfigResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static sunstone.core.SunstoneConfigResolver.getValue;

public class AzureUtils {
    static Logger LOGGER = AzureLogger.DEFAULT;
    static AzureResourceManager getResourceManager() {
        return AzureResourceManager
                .authenticate(getCredentials(), new AzureProfile(AzureEnvironment.AZURE))
                .withSubscription(SunstoneConfigResolver.getString(AzureConfig.SUBSCRIPTION_ID));
    }
    static PostgreSqlManager getPgsqlManager() {
        return PostgreSqlManager
                .authenticate(getCredentials(), new AzureProfile(SunstoneConfigResolver.getString(AzureConfig.TENANT_ID), SunstoneConfigResolver.getString(AzureConfig.SUBSCRIPTION_ID), AzureEnvironment.AZURE));
    }

    private static TokenCredential getCredentials() {
        return new ClientSecretCredentialBuilder()
                .tenantId(SunstoneConfigResolver.getString(AzureConfig.TENANT_ID))
                .clientId(SunstoneConfigResolver.getString(AzureConfig.APPLICATION_ID))
                .clientSecret(SunstoneConfigResolver.getString(AzureConfig.PASSWORD))
                .build();
    }

    static boolean propertiesForArmClientArePresent() {
        return SunstoneConfigResolver.unwrap().isPropertyPresent(AzureConfig.SUBSCRIPTION_ID)
                && SunstoneConfigResolver.unwrap().isPropertyPresent(AzureConfig.TENANT_ID)
                && SunstoneConfigResolver.unwrap().isPropertyPresent(AzureConfig.APPLICATION_ID)
                && SunstoneConfigResolver.unwrap().isPropertyPresent(AzureConfig.PASSWORD);
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

    /**
     * API is restricted by RGName but not specific RG, potentially fetching logs from previous RGs with the same name.
     */
    static void downloadResourceGroupLogs(AzureResourceManager armManager, String rgName) {
        PagedIterable<EventData> x = armManager.activityLogs().defineQuery()
                .startingFrom(OffsetDateTime.now().minusHours(getValue(AzureConfig.RG_LOGGER_TIME_START, 1)))
                .endsBefore(OffsetDateTime.now())
                .withAllPropertiesInResponse()
                .filterByResourceGroup(rgName)
                .execute();

        Path logDir = Paths.get("logs");
        if (!Files.exists(logDir)) {
            try {
                Files.createDirectories(logDir);
            } catch (IOException e) {
                LOGGER.error("Error creating log directory", e);
                return;
            }
        }

        Path activityLogFile = logDir.resolve(rgName + "-activity-log.log");
        try(java.io.FileWriter writer = new java.io.FileWriter(activityLogFile.toString())) {
            for (EventData e : x) {
                writer.write(e.operationName().localizedValue() + "\tname: " + e.eventName().localizedValue() + "\tprops: " + e.properties() + "\tdesc: " + e.description() + "\ttimestamp: " + e.eventTimestamp() + "\tlevel: " + e.level() + "\n\n");
            }
        } catch (IOException e) {
            LOGGER.error("Error writing activity log", e);
        }

        Path deploymentLogFile = logDir.resolve(rgName + "-deployments.log");
        try(java.io.FileWriter writer = new java.io.FileWriter(deploymentLogFile.toString())) {
            armManager.deployments().listByResourceGroup(rgName).forEach(d -> {
                List<String> operations = d.deploymentOperations().list().stream().map(s ->  "\n" + s.timestamp() + ", state: " + s.provisioningState() + ", op: " + s.provisioningOperation() + ", resource: " + (s.targetResource() != null ? s.targetResource().resourceName() : "UNKNOWN_NAME") + ", type: " +  (s.targetResource() != null ? s.targetResource().resourceType() : "UNKNOWN_TYPE")).collect(Collectors.toList());
                try {
                    writer.write(d.error().getMessage());
                    writer.write("\nError message: \n");
                    writer.write(d.error().getDetails().stream().map(ManagementError::getMessage).collect(Collectors.joining()));
                    writer.write("\nDeployment Info:\n");
                    writer.write(d.name() + "\t" + d.provisioningState() + "\t" + d.timestamp() + "\twith:" + operations + "\n");
                    writer.write("\n\n");
                } catch (IOException e) {
                    LOGGER.error("Error writing deployment log", e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error writing deployment log", e);
        }
    }

}
