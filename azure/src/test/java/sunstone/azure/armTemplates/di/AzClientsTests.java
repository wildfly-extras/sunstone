package sunstone.azure.armTemplates.di;


import sunstone.azure.api.AzureAutoResolve;
import com.azure.resourcemanager.AzureResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SunstoneExtension.class)
public class AzClientsTests {

    @AzureAutoResolve
    static AzureResourceManager staticArmClient;

    @AzureAutoResolve
    AzureResourceManager armClient;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticArmClient).isNotNull();
    }

    @Test
    public void testClients() {
        assertThat(armClient.resourceGroups().list().stream().collect(Collectors.toList())).isNotNull();
        assertThat(staticArmClient.resourceGroups().list().stream().collect(Collectors.toList())).isNotNull();
    }
}