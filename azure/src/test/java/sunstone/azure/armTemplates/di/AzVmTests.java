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
import static sunstone.azure.armTemplates.di.AzVmTests.group;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = group, perSuite = true)
public class AzVmTests {
    // must be same string as in MP Config
    public static final String group = "${azure.group:sunstone-testing-group}";

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static Hostname staticHostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = group)
    static Hostname staticHostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static VirtualMachine staticVM;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = group)
    static VirtualMachine staticVmWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    Hostname hostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = group)
    Hostname hostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    VirtualMachine vm;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = group)
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