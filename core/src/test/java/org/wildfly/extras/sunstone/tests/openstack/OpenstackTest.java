package org.wildfly.extras.sunstone.tests.openstack;

import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.util.Map;

import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class OpenstackTest extends AbstractCloudProviderTest {
    public OpenstackTest() throws IOException {
        super(OPENSTACK);
    }

    @BeforeClass
    public static void beforeClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("openstack.endpoint")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("openstack.username")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("openstack.password")));
    }

    private static final TestedCloudProvider OPENSTACK = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.OPENSTACK;
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
            return ImmutableMap.of("openstack.username", "INVALID-USER");
        }
    };
}
