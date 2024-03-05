package sunstone.azure.impl;


import com.azure.core.management.Region;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.fluentcore.model.Accepted;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import sunstone.core.TimeoutUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static sunstone.core.SunstoneConfig.getValue;

/**
 * Purpose: the class handles Azure template - deploy and undeploy the template to and from a stack.
 * <p>
 * Used by {@link AzureSunstoneDeployer}.
 * <p>
 * Azure ARM client credentials comes from annotation or MP Config. See {@link AzureUtils}.
 */

class AzureArmTemplateCloudDeploymentManager {
    static Logger LOGGER = AzureLogger.DEFAULT;

    private final AzureResourceManager armManager;
    private final Set<String> usedRG;

    AzureArmTemplateCloudDeploymentManager(AzureResourceManager arm) {
        armManager = arm;
        usedRG = Collections.synchronizedSet(new HashSet<>());
    }

    Set<String> getUsedRG() {
        return Collections.unmodifiableSet(usedRG);
    }

    /**
     * Returns resource group name as resources are supposed to share the lifecycle
     */
    void deploy(String template, Map<String, String> parameters, String group, String regionStr) throws IOException {
        String deploymentName = "SunstoneDeployment-" + UUID.randomUUID().toString().substring(0, 5);
        Region region = Region.fromName(regionStr);
        if (region == null) {
            throw new IllegalArgumentException("Unknown region " + regionStr);
        }

        if (!armManager.resourceGroups().contain(group)) {
            armManager.resourceGroups().define(group)
                    .withRegion(region)
                    .create();
        } else {
            LOGGER.warn("Azure resource group '{}' already exists! It will be reused and deleted when tests are finished.", group);
        }

        //.create() doesn't allow status check and fails
        Accepted<Deployment> acceptedDeployment = armManager.deployments().define(deploymentName)
                .withExistingResourceGroup(group)
                .withTemplate(template)
                .withParameters(parametersFromMap(template, parameters))
                .withMode(DeploymentMode.INCREMENTAL)
                .beginCreate();

        // polling wait
        final Duration pollInterval = Duration.ofSeconds(TimeoutUtils.adjust(getValue(AzureConfig.DEPLOY_POLL_WAIT, 1)));
        LongRunningOperationStatus pollStatus = acceptedDeployment.getActivationResponse().getStatus();
        long delayInMills = pollInterval.toMillis();
        while (!pollStatus.isComplete()) {
            try {
                Thread.sleep(delayInMills);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            PollResponse<?> pollResponse = acceptedDeployment.getSyncPoller().poll();
            pollStatus = pollResponse.getStatus();
        }
        if (pollStatus != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
            LOGGER.error("Azure deployment from template {} in \"{}\" group failed", deploymentName, group);
            AzureUtils.downloadResourceGroupLogs(armManager, group);
            undeploy(group);
            throw new RuntimeException("Deployment failed for group:" + group);
        }

        LOGGER.debug("Azure deployment from template {} in \"{}\" group is ready", deploymentName, group);
    }

    private String parametersFromMap(String template, Map<String, String> parameters) {
        Map<String, Map<String, Object>> result = new HashMap();

        JsonObject templateJSON = JsonParser.parseString(template).getAsJsonObject();
        if (templateJSON.get("parameters") == null) {
            return "{}";
        }
        JsonObject templateParamsJSON = templateJSON.get("parameters").getAsJsonObject();

        parameters.forEach((k, v) -> {
            if (templateParamsJSON.has(k)) {
                String type = templateParamsJSON.get(k).getAsJsonObject().get("type").getAsString();
                Map<String, Object> valueElement = new HashMap<>();
                switch (type.toLowerCase()) {
                    case "string":
                    case "securestring":
                        valueElement.put("value", v);
                        break;
                    case "int":
                        valueElement.put("value", Integer.parseInt(v));
                        break;
                    case "bool":
                        valueElement.put("value", Boolean.parseBoolean(v));
                        break;
                    default:
                        throw new RuntimeException(String.format("Unknown type '%s' of parameter '%s'", type, k));
                }
                result.put(k, valueElement);
            }
        });
        String r = new Gson().toJson(result);
        return r;
    }

    public void undeploy(String rgName) {
        ResourceGroups rgs = armManager.resourceGroups();
        if (rgs.contain(rgName)) {
            rgs.deleteByName(rgName);
        }
        usedRG.remove(rgName);
    }

    public void deployAndRegister(String group, String region, String templateContent, Map<String, String> parameters) throws IOException {
        deploy(templateContent, parameters, group, region);
        usedRG.add(group);
    }

    public void undeployAll() {
        usedRG.forEach(this::undeploy);
    }
}
