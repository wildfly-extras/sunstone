package sunstone.core;


import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AzureArmTemplateDeploymentManager implements TemplateDeploymentManager {

    private final AzureResourceManager armManager;
    private final Set<String> usedRG;

    private String groupName;

    AzureArmTemplateDeploymentManager() {
        armManager = AzureClientFactory.getResourceManager();
        groupName = "istraka-" + UUID.randomUUID().toString().substring(0,5);

        // we will use one resource group and delete at in the end
        armManager.resourceGroups().define(groupName)
                // todo from properties
                .withRegion(Region.US_EAST2)
                .create();

        usedRG = new HashSet<>();
    }

    Set<String> getUsedRG() {
        return Collections.unmodifiableSet(usedRG);
    }

    /**
     * Returns resource group name as resources are supposed to share the lifecycle
     */
    String deploy(String template, Map<String, String> parameters) throws IOException {
        String deploymentName = "SunstoneDeployment-" + UUID.randomUUID().toString().substring(0,5);

        armManager.deployments().define(deploymentName)
                .withExistingResourceGroup(groupName)
                .withTemplate(template)
                .withParameters(parametersFromMap(template, parameters))
                .withMode(DeploymentMode.INCREMENTAL)
                .create();
        return groupName;
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
                        throw new RuntimeException(String.format("Unknown type '%s' of parameter '%s'",type, k));
                }
                result.put(k, valueElement);
            }
        });
        return new Gson().toJson(result);
    }



    @Override
    public void undeploy(String rgName) {
        ResourceGroups rgs = armManager.resourceGroups();
        if (rgs.contain(rgName)) {
            rgs.deleteByName(rgName);
        }
    }

    @Override
    public void register(String id) {
        usedRG.add(id);
    }

    public void close() {
        // nothing to do
    }

    @Override
    public void deployAndRegister(String templateContent, Map<String, String> parameters) throws IOException {
        register(deploy(templateContent, parameters));
    }

    @Override
    public void undeplyAll() {
        usedRG.forEach(this::undeploy);
    }
}
