package azure.armTemplates.suite.tests;


import azure.armTemplates.AzureTestUtils;
import azure.armTemplates.AwsTestConstants;
import azure.armTemplates.suite.AzureArmTemplatesSuiteTest;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.api.WithAzureArmTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AwsTestConstants.VNET_NAME_1),
        @Parameter(k = "vnetTag", v = AwsTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = AzureArmTemplatesSuiteTest.GROUP, perSuite = true)
@WithAzureArmTemplate(parameters = {
        @Parameter(k = "vnetName", v = AwsTestConstants.VNET_NAME_2),
        @Parameter(k = "vnetTag", v = AwsTestConstants.VNET_TAG)
},
        template = "azure/armTemplates/vnet.json", region = "eastus2", group = AzureArmTemplatesSuiteTest.GROUP, perSuite = true)
public class TwoSamePerSuiteAzArmTemplates {
    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourceCreated() {
        Network vnet = arm.networks().getByResourceGroup(AzureArmTemplatesSuiteTest.GROUP, AwsTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
        // same (same sum) template should not produce another resource even if PARAMETRIZED name is different
        PagedIterable<Network> networks = arm.networks().listByResourceGroup(AzureArmTemplatesSuiteTest.GROUP);
        assertThat(networks).noneMatch(network -> network.name().equals(AwsTestConstants.VNET_NAME_2));
    }
}