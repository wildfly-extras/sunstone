package azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;

import static azure.armTemplates.SingleAzArmTemplateTest.GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
        },
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = GROUP)
public class SingleAzArmTemplateTest {
    static final String GROUP = "SingleAzArmTemplateTest";

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourceCreated() {
        Network vnet = arm.networks().getByResourceGroup(GROUP, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
    }
}
