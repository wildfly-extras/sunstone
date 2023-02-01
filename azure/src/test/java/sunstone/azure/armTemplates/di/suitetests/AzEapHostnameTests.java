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
public class AzEapHostnameTests {

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static Hostname staticEapHostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group)
    static Hostname staticEapHostnameWithRegion;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    Hostname eapHostname;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group)
    Hostname eapHostnameWithRegion;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticEapHostname).isNotNull();
        assertThat(staticEapHostnameWithRegion).isNotNull();
    }

    @Test
    public void hostnamesNotEmpty() {
        assertThat(staticEapHostname.get()).isNotBlank();
        assertThat(staticEapHostnameWithRegion.get()).isNotBlank();
        assertThat(eapHostname.get()).isNotBlank();
        assertThat(eapHostnameWithRegion.get()).isNotBlank();
    }
}