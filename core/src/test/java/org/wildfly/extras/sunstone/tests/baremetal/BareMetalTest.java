package org.wildfly.extras.sunstone.tests.baremetal;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

public class BareMetalTest extends AbstractCloudProviderTest {
    public BareMetalTest() throws IOException {
        super(BARE_METAL);
    }

    @BeforeClass
    public static void beforeClass() {
        assumeTrue(System.getProperty("baremetal.run") != null);
    }

    private static final TestedCloudProvider BARE_METAL = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.BARE_METAL;
        }

        @Override
        public boolean hasImages() {
            return false;
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
            return false;
        }

        @Override
        public boolean fileCopyingSupported() {
            return true;
        }

        @Override
        public Map<String, String> overridesThatPreventCreatingNode() {
            return null;
        }
    };
}
