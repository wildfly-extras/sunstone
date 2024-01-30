package sunstone.azure.armTemplates.di;


import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.azure.annotation.AzurePgSqlServer;
import sunstone.azure.annotation.WithAzureArmTemplate;
import sunstone.inject.Hostname;

import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.AzureTestConstants.deployGroup;


@WithAzureArmTemplate(parameters = {
        @Parameter(k = "administratorLogin", v = AzPgSqlTests.PGSQL_USER),
        @Parameter(k = "administratorLoginPassword", v = AzPgSqlTests.PGSQL_PASSWORD),
        @Parameter(k = "serverName", v = AzPgSqlTests.PGSQL_NAME),
},
        template = "sunstone/azure/armTemplates/posgresql.json", group = AzPgSqlTests.groupName)
public class AzPgSqlTests {
    static final String groupName = "AzPgSqlTests-" + deployGroup;
    public static final String PGSQL_NAME = "ds-azure-test-pgsql-${ts.test.run}";
    public static final String PGSQL_USER = "user";
    public static final String PGSQL_PASSWORD = "1234567890Ab";

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    static Hostname staticHostname;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    static Hostname staticHostnameWithRegion;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    static Server staticServer;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    static Server staticServerWithGroup;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    Hostname hostname;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    Hostname hostnameWithGroup;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
    Server vm;

    @AzurePgSqlServer(name = AzPgSqlTests.PGSQL_NAME, group = groupName)
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