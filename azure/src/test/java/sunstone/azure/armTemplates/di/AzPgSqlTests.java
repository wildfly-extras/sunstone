package sunstone.azure.armTemplates.di;


import com.azure.resourcemanager.postgresql.models.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.azure.annotation.AzurePgSqlServer;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.inject.Hostname;

import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.di.AzVmTests.group;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "username", v = AzPgSqlTests.PGSQL_USER),
        @Parameter(k = "password", v = AzPgSqlTests.PGSQL_PASSWORD),
        @Parameter(k = "serverName", v = AzPgSqlTests.PGSQL_NAME),
},
        template = "sunstone/azure/armTemplates/posgresql.json", group = group, perSuite = true)
public class AzPgSqlTests {
    public static final String PGSQL_NAME = "DSAzureTest-pgsql";
    public static final String PGSQL_USER = "user";
    public static final String PGSQL_PASSWORD = "1234567890Ab";

    // must be same string as in MP Config
    public static final String group = "${azure.group:sunstone-testing-group}";

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME)
    static Hostname staticHostname;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = group)
    static Hostname staticHostnameWithRegion;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME)
    static Server staticServer;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = group)
    static Server staticServerWithGroup;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME)
    Hostname hostname;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = group)
    Hostname hostnameWithGroup;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME)
    Server vm;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = group)
    Server vmWithGroup;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticServer).isNotNull();
        assertThat(staticServerWithGroup).isNotNull();
        assertThat(staticHostname).isNotNull();
        assertThat(staticHostnameWithRegion).isNotNull();
    }

    @Test
    public void testDI() {
        assertThat(staticServer.id()).isNotBlank();
        assertThat(staticServerWithGroup.id()).isNotBlank();
        assertThat(vm.id()).isNotBlank();
        assertThat(vmWithGroup.id()).isNotBlank();
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(staticHostnameWithRegion.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
        assertThat(hostnameWithGroup.get()).isNotBlank();
    }
}