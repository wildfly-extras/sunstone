package sunstone.core;


import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Purpose: the class handles Azure template - deploy and undeploy the template to and from a stack.
 *
 * Used by {@link SunstoneCloudDeploy}. Deploys to a group defined in Sunstone.properties (which is deleted as a whole later).
 *
 * Azure ARM client credentials are taken from Sunstone.properties. See {@link AzureUtils}.
 */

class AzureArmTemplateCloudDeploymentManager implements TemplateCloudDeploymentManager {
    static Logger LOGGER = SunstoneJUnit5Logger.DEFAULT;

    private final AzureResourceManager armManager;
    private final Set<String> usedRG;

    // everything is deployed to one resource group
    private String groupName;

    AzureArmTemplateCloudDeploymentManager(AzureResourceManager arm) {
        ObjectProperties objectProperties = new ObjectProperties(ObjectType.JUNIT5, null);
        armManager = arm;
        groupName = objectProperties.getProperty(JUnit5Config.JUnit5.Azure.GROUP);
        Region region = Region.fromName(new ObjectProperties(ObjectType.JUNIT5, null).getProperty(JUnit5Config.JUnit5.Azure.REGION));

        // we will use one resource group and delete at in the end
        if (!armManager.resourceGroups().contain(groupName)){
            armManager.resourceGroups().define(groupName)
                    .withRegion(region)
                    .create();
        }
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
        LOGGER.debug("Azure deployment from template {} in \"{}\" group is ready", deploymentName, groupName);
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
        String r = new Gson().toJson(result);
        return r;
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
        // nothing to close
    }

    @Override
    public void deployAndRegister(String templateContent, Map<String, String> parameters) throws IOException {
        register(deploy(templateContent, parameters));
    }

    @Override
    public void undeployAll() {
        usedRG.forEach(this::undeploy);
        usedRG.clear();
    }
}
