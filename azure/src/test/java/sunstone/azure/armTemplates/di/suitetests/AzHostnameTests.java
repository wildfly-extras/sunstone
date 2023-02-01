package sunstone.azure.armTemplates.di.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.di.SunstoneResourceAzSuiteTests;
import sunstone.azure.api.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import sunstone.azure.api.WithAzureArmTemplate;
import sunstone.api.inject.Hostname;

import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = SunstoneResourceAzSuiteTests.group, perSuite = true)
public class AzHostnameTests {

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static Hostname staticHostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group)
    static Hostname staticHostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    Hostname hostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group)
    Hostname hostnameWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticHostname).isNotNull();
        assertThat(staticHostnameWithRegion).isNotNull();
    }

    @Test
    public void hostnamesNotEmpty() {
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(staticHostnameWithRegion.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
        assertThat(hostnameWithRegion.get()).isNotBlank();
    }
}