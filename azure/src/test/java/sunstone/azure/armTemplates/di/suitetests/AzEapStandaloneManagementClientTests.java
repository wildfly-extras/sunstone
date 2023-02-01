package sunstone.azure.armTemplates.di.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.di.SunstoneResourceAzSuiteTests;
import sunstone.azure.api.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.EapMode;
import sunstone.api.Parameter;
import sunstone.api.StandaloneMode;
import sunstone.azure.api.WithAzureArmTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = SunstoneResourceAzSuiteTests.group, perSuite = true)
public class AzEapStandaloneManagementClientTests {

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static OnlineManagementClient staticEapClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.eapMngmtUser, password = AzureTestConstants.eapMngmtPassword, port = AzureTestConstants.eapMngmtPort))
    static OnlineManagementClient staticEapClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    OnlineManagementClient eapClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.eapMngmtUser, password = AzureTestConstants.eapMngmtPassword, port = AzureTestConstants.eapMngmtPort))
    OnlineManagementClient eapClientSpecified;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticEapClient).isNotNull();
        assertThat(staticEapClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticEapClient.execute(":whoami").assertSuccess();
        staticEapClientSpecified.execute(":whoami").assertSuccess();
        eapClient.execute(":whoami").assertSuccess();
        eapClientSpecified.execute(":whoami").assertSuccess();
    }
}