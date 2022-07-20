package org.jboss.eapqe.clouds.ec2;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import sunstone.core.AzureClientFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AzureTest {

    @Test
    public void test() throws IOException {

        OnlineManagementClient admin = ManagementClient.online(OnlineOptions.standalone()
                .hostAndPort("3.237.103.111", 9990)
                .auth("admin", "pass.1234")
                .connectionTimeout(6000)
                .bootTimeout(1000 * (int) 60)
                .build());
        Map<String, String> m = new HashMap<>();
        m.put("nic-name3", "1");
        m.put("nic-name2", "omgg");
//        System.out.println(AzureArmTemplateDeploymentManager.parametersFromMap(template, m));
        JsonObject element = JsonParser.parseString(template).getAsJsonObject();
        element.get("parameters");

        AzureResourceManager resourceManager = AzureClientFactory.getResourceManager();
    }

    String template = "{\n"
            + "  \"$schema\": \"https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#\",\n"
            + "  \"contentVersion\": \"1.0.0.0\",\n"
            + "  \"parameters\": {\n"
            + "    \"nic-name\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"Name of the nic to add a public IP\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"ip-config-name\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"Name of the ip config to associate a public IP\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"public-ip-id\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"ID of public IP\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"subnet-name\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"ID of subnet where is the interface\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"vnet-id\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"ID of vnet where is the interface\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"location\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"defaultValue\": \"[resourceGroup().location]\",\n"
            + "      \"metadata\": {\n"
            + "        \"description\": \"Location for all resources.\"\n"
            + "      }\n"
            + "    }\n"
            + "  },\n"
            + "  \"variables\": {\n"
            + "    \"subnetRef\": \"[concat(parameters('vnet-id'), '/subnets/', parameters('subnet-name'))]\"\n"
            + "  },\n"
            + "  \"resources\": [\n"
            + "    {\n"
            + "      \"apiVersion\": \"2019-09-01\",\n"
            + "      \"name\": \"[parameters('nic-name')]\",\n"
            + "      \"location\": \"[parameters('location')]\",\n"
            + "      \"properties\": {\n"
            + "        \"ipConfigurations\": [\n"
            + "          {\n"
            + "            \"name\": \"[parameters('ip-config-name')]\",\n"
            + "            \"type\": \"Microsoft.Network/networkInterfaces/ipConfigurations\",\n"
            + "            \"properties\": {\n"
            + "              \"publicIPAddress\": {\n"
            + "                \"id\": \"[parameters('public-ip-id')]\"\n"
            + "              },\n"
            + "              \"subnet\": {\n"
            + "                \"id\": \"[variables('subnetRef')]\"\n"
            + "              }\n"
            + "            }\n"
            + "          }\n"
            + "        ]\n"
            + "      },\n"
            + "      \"type\": \"Microsoft.Network/networkInterfaces\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";
}
