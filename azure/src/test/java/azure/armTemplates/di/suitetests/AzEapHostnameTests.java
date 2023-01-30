package azure.armTemplates.di.suitetests;


import azure.core.identification.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.api.Parameter;
import azure.api.WithAzureArmTemplate;
import sunstone.api.inject.Hostname;

import static azure.armTemplates.AzureTestConstants.IMAGE_REF;
import static azure.armTemplates.di.SunstoneResourceAzSuiteTests.group;
import static azure.armTemplates.AzureTestConstants.instanceName;
import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = instanceName),
        @Parameter(k = "imageRefId", v = IMAGE_REF)
},
        template = "azure/armTemplates/eap.json", group = group, perSuite = true)
public class AzEapHostnameTests {

    @AzureVirtualMachine(name = instanceName)
    static Hostname staticEapHostname;

    @AzureVirtualMachine(name = instanceName, group = group)
    static Hostname staticEapHostnameWithRegion;

    @AzureVirtualMachine(name = instanceName)
    Hostname eapHostname;

    @AzureVirtualMachine(name = instanceName, group = group)
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