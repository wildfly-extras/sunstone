package sunstone.azure.armTemplates.di;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.Parameter;
import sunstone.annotation.WildFly;
import sunstone.azure.annotation.AzureVirtualMachine;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.azure.armTemplates.AzureTestConstants;
import static org.assertj.core.api.Assertions.assertThat;

@WithAzureArmTemplate(
        parameters = {
            @Parameter(k = "virtualMachineName", v = AzureTestConstants.instanceName),
            @Parameter(k = "imageRefId", v = AzureTestConstants.IMAGE_REF)
        },
        template = "sunstone/azure/armTemplates/eapDomain.json", group = AzDomainManagementClientTests.group, perSuite = true)
public class AzDomainManagementClientTests {

    public static final String group = "${azure.group:sunstone-testing-group}";

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
    static OnlineManagementClient staticMngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzDomainManagementClientTests.group)
    @WildFly(mode = OperatingMode.DOMAIN, domain = @DomainMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort, host = AzureTestConstants.mngmtHost, profile = AzureTestConstants.mngmtProfile))
    static OnlineManagementClient staticMngmtClientSpecified;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName)
    @WildFly(mode = OperatingMode.DOMAIN)
    OnlineManagementClient mngmtClient;

    @AzureVirtualMachine(name = AzureTestConstants.instanceName, group = AzDomainManagementClientTests.group)
    @WildFly(mode = OperatingMode.DOMAIN, domain = @DomainMode(user = AzureTestConstants.mngmtUser, password = AzureTestConstants.mngmtPassword, port = AzureTestConstants.mngmtPort, host = AzureTestConstants.mngmtHost, profile = AzureTestConstants.mngmtProfile))
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
