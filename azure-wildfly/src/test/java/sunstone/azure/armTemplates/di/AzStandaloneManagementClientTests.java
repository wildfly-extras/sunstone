package sunstone.azure.armTemplates.di;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.StandaloneMode;
import sunstone.annotation.WildFly;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
        @Parameter(k = "planName", v = AzureTestConstants.IMAGE_MARKETPLACE_PLAN),
        @Parameter(k = "publisher", v = AzureTestConstants.IMAGE_MARKETPLACE_PUBLISHER),
        @Parameter(k = "product", v = AzureTestConstants.IMAGE_MARKETPLACE_PRODUCT),
        @Parameter(k = "offer", v = AzureTestConstants.IMAGE_MARKETPLACE_OFFER),
        @Parameter(k = "sku", v = AzureTestConstants.IMAGE_MARKETPLACE_SKU),
        @Parameter(k = "version", v = AzureTestConstants.IMAGE_MARKETPLACE_VERSION),
},
        template = "sunstone/azure/armTemplates/eap.json", group = AzStandaloneManagementClientTests.groupName)
public class AzStandaloneManagementClientTests {
    static final String groupName = "AzStandaloneManagementClientTests-" + AzureTestConstants.deployGroup;
    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    static OnlineManagementClient staticMgmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    @WildFly(mode = OperatingMode.STANDALONE, standalone = @StandaloneMode(user = AzureTestConstants.mgmtUser, password = AzureTestConstants.mgmtPassword, port = AzureTestConstants.mgmtPort))
    static OnlineManagementClient staticMgmtClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
    OnlineManagementClient mgmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = groupName)
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
