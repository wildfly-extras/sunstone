package org.wildfly.extras.sunstone.tests.azurearm;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assume.assumeFalse;

public class AzureArmTest extends AbstractCloudProviderTest {
    public AzureArmTest() throws IOException {
        super(AZURE_ARM);
    }

    @BeforeClass
    public static void beforeClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure-arm.subscriptionId")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure-arm.tenantId")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure-arm.applicationId")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure-arm.password")));
    }

    private static final TestedCloudProvider AZURE_ARM = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.AZURE_ARM;
        }

        @Override
        public boolean hasImages() {
            return true;
        }

        @Override
        public boolean hasPortMapping() {
            return false;
        }

        @Override
        public boolean commandExecutionSupported() {
            return true;
        }

        @Override
        public boolean execBuilderSupported() {
            return true;
        }

        @Override
        public boolean lifecycleControlSupported() {
            return true;
        }

        @Override
        public boolean fileCopyingSupported() {
            return true;
        }

        @Override
        public Map<String, String> overridesThatPreventCreatingNode() {
            return ImmutableMap.of("azure.subscriptionId", "INVALID-SUBSCRIPTION-ID");
        }
    };
}
