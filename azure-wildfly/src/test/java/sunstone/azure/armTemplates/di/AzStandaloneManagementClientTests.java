package sunstone.azure.armTemplates.di;


import sunstone.annotation.WildFly;
import sunstone.azure.armTemplates.AzureTestConstants;
import sunstone.azure.annotation.AzureVirtualMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.StandaloneMode;
import sunstone.azure.annotation.WithAzureArmTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
},
        template = "sunstone/azure/armTemplates/eap.json", group = AzStandaloneManagementClientTests.group, perSuite = true)
public class AzStandaloneManagementClientTests {
    public static final String group = "${azure.group:sunstone-testing-group}";

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    static OnlineManagementClient staticMngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzStandaloneManagementClientTests.group)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort))
    static OnlineManagementClient staticMngmtClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    OnlineManagementClient mngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzStandaloneManagementClientTests.group)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort))
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
