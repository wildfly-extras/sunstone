package sunstone.azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.annotation.WithAzureArmTemplateRepeatable;

import static sunstone.azure.armTemplates.MultipleAzArmTemplatesRepetableTest.GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplateRepeatable({
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
                @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
        },
                template = "sunstone/azure/armTemplates/vnet.json", region = "eastus2", group = GROUP),
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
                @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
        },
                template = "sunstone/azure/armTemplates/vnet.json", region = "eastus2", group = GROUP)
})
public class MultipleAzArmTemplatesRepetableTest {
    static final String GROUP = "MultipleAzArmTemplatesRepetableTest";

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(GROUP, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
        vnet = arm.networks().getByResourceGroup(GROUP, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}
