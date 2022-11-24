package azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;
import sunstone.api.WithAzureArmTemplateRepeatable;

import static azure.armTemplates.MultipleAzArmTemplatesRepetableTest.GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplateRepeatable({
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AwsTestConstants.VNET_NAME_1),
                @Parameter(k = "vnetTag", v = AwsTestConstants.VNET_TAG)
        },
                template = "azure/armTemplates/vnet.json", region = "eastus2", group = GROUP),
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AwsTestConstants.VNET_NAME_2),
                @Parameter(k = "vnetTag", v = AwsTestConstants.VNET_TAG)
        },
                template = "azure/armTemplates/vnet.json", region = "eastus2", group = GROUP)
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
        Network vnet = arm.networks().getByResourceGroup(GROUP, AwsTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
        vnet = arm.networks().getByResourceGroup(GROUP, AwsTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}
