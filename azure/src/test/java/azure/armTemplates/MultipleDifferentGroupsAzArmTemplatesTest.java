package azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;

import static azure.armTemplates.MultipleDifferentGroupsAzArmTemplatesTest.GROUP1;
import static azure.armTemplates.MultipleDifferentGroupsAzArmTemplatesTest.GROUP2;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = GROUP1)

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = GROUP2)
public class MultipleDifferentGroupsAzArmTemplatesTest {
    static final String GROUP1 = "MultipleAzArmTemplatesTest1";
    static final String GROUP2 = "MultipleAzArmTemplatesTest2";

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void group1ResourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(GROUP1, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
    }
    @Test
    public void group2ResourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(GROUP2, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}