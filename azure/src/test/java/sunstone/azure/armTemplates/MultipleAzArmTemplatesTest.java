package sunstone.azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneProperty;
import sunstone.azure.annotation.WithAzureArmTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.AzureTestConstants.deployGroup;
import static sunstone.azure.armTemplates.MultipleAzArmTemplatesTest.groupName;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", group = groupName)

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", group = groupName)
public class MultipleAzArmTemplatesTest {
    static final String groupName = "MultipleAzArmTemplatesTest-" + deployGroup;

    @SunstoneProperty(expression = groupName)
    static String classGroup;

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourcesCreated() {
        Network vnet1 = arm.networks().getByResourceGroup(classGroup, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet1).isNotNull();
        Network vnet2 = arm.networks().getByResourceGroup(classGroup, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet2).isNotNull();
    }
}