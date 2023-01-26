package azure.armTemplates.suite.tests;


import azure.armTemplates.AzureTestUtils;
import azure.armTemplates.AzureTestConstants;
import azure.armTemplates.suite.AzureArmTemplatesSuiteTest;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = AzureArmTemplatesSuiteTest.GROUP, perSuite = true)
@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = PerSuitePerClassAzArmTemplates.GROUP, perSuite = false)
public class PerSuitePerClassAzArmTemplates {
    static final String GROUP = "PerSuitePerClassAzArmTemplate";
    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourceCreated() {
        Network vnet = arm.networks().getByResourceGroup(AzureArmTemplatesSuiteTest.GROUP, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
        vnet = arm.networks().getByResourceGroup(PerSuitePerClassAzArmTemplates.GROUP, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}
