package sunstone.azure.armTemplates.suite.tests;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneProperty;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.AzureTestUtils;
import sunstone.azure.armTemplates.suite.AzureArmTemplatesSuiteTest;

import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", group = AzureArmTemplatesSuiteTest.groupName, perSuite = true)
public class PerSuiteAzArmTemplate {
    static AzureResourceManager arm;

    @SunstoneProperty(expression=AzureArmTemplatesSuiteTest.groupName)
    static String suiteGroup;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourceCreated() {
        Network vnet = arm.networks().getByResourceGroup(suiteGroup, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
    }
}
