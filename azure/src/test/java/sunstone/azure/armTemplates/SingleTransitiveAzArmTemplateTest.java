package sunstone.azure.armTemplates;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sunstone.annotation.Parameter;
import sunstone.annotation.SunstoneProperty;
import sunstone.azure.annotation.WithAzureArmTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import static sunstone.azure.armTemplates.AzureTestConstants.deployGroup;

@SingleTransitiveAzArmTemplateTest.TestTransitiveAnnotation
public class SingleTransitiveAzArmTemplateTest {
    static final String groupName = "SingleTransitiveAzArmTemplateTest-" + deployGroup;

    @SunstoneProperty(expression = groupName)
    static String classGroup;

    static AzureResourceManager arm;

    @BeforeAll
    public static void setup() {
        arm = AzureTestUtils.getResourceManager();
    }

    @Test
    public void resourceCreated() {
        Network vnet = arm.networks().getByResourceGroup(classGroup, AzureTestConstants.VNET_NAME_1);
        assertThat(vnet).isNotNull();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @WithAzureArmTemplate(parameters = {
            @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
            @Parameter(k = "vnetTag", v = AzureTestConstants.VNET_TAG)
    },
            template = "sunstone/azure/armTemplates/vnet.json", group = groupName)
    @interface TestTransitiveAnnotation {
    }
}
