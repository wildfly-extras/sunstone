package aws.cloudformation.di.suitetests;


import static org.assertj.core.api.Assertions.assertThat;

import aws.cloudformation.AwsTestConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.rds.model.DBInstance;
import sunstone.annotation.Parameter;
import sunstone.aws.annotation.AwsRds;
import sunstone.aws.annotation.WithAwsCfTemplate;
import sunstone.inject.Hostname;


@WithAwsCfTemplate(parameters = {
        @Parameter(k = "username", v = AwsRdsTests.PGSQL_USER),
        @Parameter(k = "password", v = AwsRdsTests.PGSQL_PASSWORD),
        @Parameter(k = "serverName", v = AwsRdsTests.PGSQL_NAME)
},
        template = "sunstone/aws/cloudformation/postgresql.yaml"
)
public class AwsRdsTests {
    // note that RDS instance must be lower case
    public static final String PGSQL_NAME = "awstest-pgsql-${ts.test.run}";
    public static final String PGSQL_USER = "testUser";
    public static final String PGSQL_PASSWORD = "1234567890Ab";

    @AwsRds(name = PGSQL_NAME)
    static Hostname staticHostname;

    @AwsRds(name = PGSQL_NAME, region = AwsTestConstants.region)
    static Hostname staticHostnameWithRegion;

    @AwsRds(name = PGSQL_NAME)
    static DBInstance staticServer;

    @AwsRds(name = PGSQL_NAME, region = AwsTestConstants.region)
    static DBInstance staticServerWithGroup;

    @AwsRds(name = PGSQL_NAME)
    Hostname hostname;

    @AwsRds(name = PGSQL_NAME, region = AwsTestConstants.region)
    Hostname hostnameWithGroup;

    @AwsRds(name = PGSQL_NAME)
    DBInstance vm;

    @AwsRds(name = PGSQL_NAME, region = AwsTestConstants.region)
    DBInstance vmWithGroup;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticServer).isNotNull();
        assertThat(staticServerWithGroup).isNotNull();
        assertThat(staticHostname).isNotNull();
        assertThat(staticHostnameWithRegion).isNotNull();
    }

    @Test
    public void testDI() {
        assertThat(staticServer.dbInstanceIdentifier()).isNotBlank();
        assertThat(staticServerWithGroup.dbInstanceIdentifier()).isNotBlank();
        assertThat(vm.dbInstanceIdentifier()).isNotBlank();
        assertThat(vmWithGroup.dbInstanceIdentifier()).isNotBlank();
        assertThat(staticHostname.get()).isNotBlank();
        assertThat(staticHostnameWithRegion.get()).isNotBlank();
        assertThat(hostname.get()).isNotBlank();
        assertThat(hostnameWithGroup.get()).isNotBlank();
    }
}
