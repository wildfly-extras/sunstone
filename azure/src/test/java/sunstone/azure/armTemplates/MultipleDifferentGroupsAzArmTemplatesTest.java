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
import static sunstone.azure.armTemplates.MultipleDifferentGroupsAzArmTemplatesTest.groupName1;
import static sunstone.azure.armTemplates.MultipleDifferentGroupsAzArmTemplatesTest.groupName2;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", group = groupName1)

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
},
        template = "sunstone/azure/armTemplates/vnet.json", group = groupName2)
public class MultipleDifferentGroupsAzArmTemplatesTest {
    static final String groupName1 = "MultipleAzArmTemplatesTest1-" + deployGroup;
    static final String groupName2 = "MultipleAzArmTemplatesTest2-" + deployGroup;

    @SunstoneProperty(expression = groupName1)
    static String classGroup1;
    @SunstoneProperty(expression = groupName2)
    static String classGroup2;

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void group1ResourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(classGroup1, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
    }
    @Test
    public void group2ResourcesCreated() {
        Network vnet = arm.networks().getByResourceGroup(classGroup2, AzureTestConstants.VNET_NAME_2);
        assertThat(vnet).isNotNull();
    }
}