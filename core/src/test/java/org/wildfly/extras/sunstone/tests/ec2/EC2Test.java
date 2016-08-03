package org.wildfly.extras.sunstone.tests.ec2;

import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.util.Map;

import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class EC2Test extends AbstractCloudProviderTest {
    public EC2Test() throws IOException {
        super(EC2);
    }

    @BeforeClass
    public static void beforeClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.accessKeyID")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.secretAccessKey")));
    }

    private static final TestedCloudProvider EC2 = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.EC2;
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
            // the following fails with the image we are using in the test:
            //
            //     ExecBuilder.fromCommand("touch", "/tmp/bagr").asDaemon().exec(node)
            //
            // even if it works OK without the 'asDaemon' part
            //
            // it works correctly with other images, so just skipping the test for now
            return false;
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
            return ImmutableMap.of("ec2.accessKeyID", "INVALID-ACCESS-KEY-ID");
        }
    };
}
