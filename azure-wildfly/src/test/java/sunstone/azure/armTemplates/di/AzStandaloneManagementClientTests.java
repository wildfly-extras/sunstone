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
    static OnlineManagementClient staticMgmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzStandaloneManagementClientTests.group)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mgmtUser, password = AzureTestConstants.mgmtPassword, port = AzureTestConstants.mgmtPort))
    static OnlineManagementClient staticMgmtClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    OnlineManagementClient mgmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzStandaloneManagementClientTests.group)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mgmtUser, password = AzureTestConstants.mgmtPassword, port = AzureTestConstants.mgmtPort))
    OnlineManagementClient mgmtClientSpecified;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticMgmtClient).isNotNull();
        assertThat(staticMgmtClientSpecified).isNotNull();
    }

    @Test
    public void testManagementClients() throws CliException, IOException {
        staticMgmtClient.execute(":whoami").assertSuccess();
        staticMgmtClientSpecified.execute(":whoami").assertSuccess();
        mgmtClient.execute(":whoami").assertSuccess();
        mgmtClientSpecified.execute(":whoami").assertSuccess();
    }
}
