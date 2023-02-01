package sunstone.azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.azure.api.WithAzureArmTemplate;

import static sunstone.azure.armTemplates.MultipleAzArmTemplatesTest.GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", region = "eastus2", group = GROUP)

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", region = "eastus2", group = GROUP)
public class MultipleAzArmTemplatesTest {
    static final String GROUP = "MultipleAzArmTemplatesTest";

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourcesCreated() {
        Network vnet1 = arm.networks().getByResourceGroup(GROUP, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet1).isNotNull();
        Network vnet2 = arm.networks().getByResourceGroup(GROUP, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet2).isNotNull();
    }
}