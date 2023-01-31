package azure.armTemplates.di.suitetests;


import azure.core.identification.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.EapMode;
import sunstone.api.Parameter;
import sunstone.api.StandaloneMode;
import azure.api.WithAzureArmTemplate;

import java.io.IOException;

import static azure.armTemplates.AzureTestConstants.IMAGE_REF;
import static azure.armTemplates.AzureTestConstants.eapMngmtPassword;
import static azure.armTemplates.AzureTestConstants.eapMngmtPort;
import static azure.armTemplates.AzureTestConstants.eapMngmtUser;
import static azure.armTemplates.di.SunstoneResourceAzSuiteTests.group;
import static azure.armTemplates.AzureTestConstants.instanceName;
import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = instanceName),
        @Parameter(k = "imageRefId", v = IMAGE_REF)
},
        template = "azure/armTemplates/eap.json", group = group, perSuite = true)
public class AzEapStandaloneManagementClientTests {

    @AzureVirtualMachine(name = instanceName)
    static OnlineManagementClient staticEapClient;

    @AzureVirtualMachine(name = instanceName, group = group, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = eapMngmtUser, password = eapMngmtPassword, port = eapMngmtPort))
    static OnlineManagementClient staticEapClientSpecified;

    @AzureVirtualMachine(name = instanceName)
    OnlineManagementClient eapClient;

    @AzureVirtualMachine(name = instanceName, group = group, mode = EapMode.STANDALONE, standalone = @StandaloneMode(user = eapMngmtUser, password = eapMngmtPassword, port = eapMngmtPort))
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