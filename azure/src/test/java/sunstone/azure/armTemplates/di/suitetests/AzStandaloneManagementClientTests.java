package sunstone.azure.armTemplates.di.suitetests;


import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.armTemplates.di.SunstoneResourceAzSuiteTests;
import sunstone.azure.api.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.OperatingMode;
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
public class AzStandaloneManagementClientTests {

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static OnlineManagementClient staticMngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group, mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort))
    static OnlineManagementClient staticMngmtClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    OnlineManagementClient mngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = SunstoneResourceAzSuiteTests.group, mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort))
    OnlineManagementClient mngmtClientSpecified;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticMngmtClient).isNotNull();
        assertThat(staticMngmtClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticMngmtClient.execute(":whoami").assertSuccess();
        staticMngmtClientSpecified.execute(":whoami").assertSuccess();
        mngmtClient.execute(":whoami").assertSuccess();
        mngmtClientSpecified.execute(":whoami").assertSuccess();
    }
}