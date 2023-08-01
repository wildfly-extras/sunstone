package sunstone.azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneProperty;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.annotation.WithAzureArmTemplateRepeatable;
import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.AzureTestConstants.deployGroup;
import static sunstone.azure.armTemplates.MultipleAzArmTemplatesRepetableTest.groupName;

@WithAzureArmTemplateRepeatable({
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
                @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
        },
                template = "sunstone/azure/armTemplates/vnet.json", group = groupName),
        @WithAzureArmTemplate(parameters = {
                @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
                @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
        },
                template = "sunstone/azure/armTemplates/vnet.json", group = groupName)
})
public class MultipleAzArmTemplatesRepetableTest {
    static final String groupName = "MultipleAzArmTemplatesRepetableTest-" + deployGroup;

    @SunstoneProperty(expression = groupName)
    static String classGroup;

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(classGroup, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
        vnet = arm.networks().getByResourceGroup(classGroup, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}
