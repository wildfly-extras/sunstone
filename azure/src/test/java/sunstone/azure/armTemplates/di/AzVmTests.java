package sunstone.azure.armTemplates.di;


import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.inject.Hostname;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.AzureTestConstants.deployGroup;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "planName", v = AzureTestConstants.IMAGE_MARKETPLACE_PLAN),
        @Parameter(k = "publisher", v = AzureTestConstants.IMAGE_MARKETPLACE_PUBLISHER),
        @Parameter(k = "product", v = AzureTestConstants.IMAGE_MARKETPLACE_PRODUCT),
        @Parameter(k = "offer", v = AzureTestConstants.IMAGE_MARKETPLACE_OFFER),
        @Parameter(k = "sku", v = AzureTestConstants.IMAGE_MARKETPLACE_SKU),
        @Parameter(k = "version", v = AzureTestConstants.IMAGE_MARKETPLACE_VERSION),
},
        template = "sunstone/azure/armTemplates/eap.json", group = AzVmTests.groupName)
public class AzVmTests {
    static final String groupName = "AzVmTests-" + deployGroup;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    static Hostname staticHostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    static Hostname staticHostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    static VirtualMachine staticVM;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    static VirtualMachine staticVmWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    Hostname hostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    Hostname hostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    VirtualMachine vm;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    VirtualMachine vmWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticVM).isNotNull();
        assertThat(staticVmWithRegion).isNotNull();
        assertThat(staticHostname).isNotNull();
        assertThat(staticHostnameWithRegion).isNotNull();
    }

    @Test
    public void testDI() {
        assertThat(staticVM.id()).isNotBlank();
        assertThat(staticVmWithRegion.id()).isNotBlank();
        assertThat(vm.id()).isNotBlank();
        assertThat(vmWithRegion.id()).isNotBlank();
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(staticHostnameWithRegion.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
        assertThat(hostnameWithRegion.get()).isNotBlank();
    }
}
