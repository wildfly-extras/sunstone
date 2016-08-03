package org.wildfly.extras.sunstone.tests.azure;

import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.util.Map;

import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class AzureTest extends AbstractCloudProviderTest {
    public AzureTest() throws IOException {
        super(AZURE);
    }

    @BeforeClass
    public static void beforeClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.subscriptionId")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.privateKeyFile")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.privateKeyPassword")));
    }

    private static final TestedCloudProvider AZURE = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.AZURE;
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
