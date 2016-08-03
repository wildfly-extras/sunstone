package org.wildfly.extras.sunstone.tests.docker;

import java.io.IOException;
import java.util.Map;

import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

import com.google.common.collect.ImmutableMap;

public class DockerTest extends AbstractCloudProviderTest {
    public DockerTest() throws IOException {
        super(DOCKER);
    }

    private static final TestedCloudProvider DOCKER = new TestedCloudProvider() {
        @Override
        public CloudProviderType type() {
            return CloudProviderType.DOCKER;
        }

        @Override
        public boolean hasImages() {
            return true;
        }

        @Override
        public boolean hasPortMapping() {
            return true;
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
            return ImmutableMap.of("docker.endpoint", "http://127.0.0.1:22/");
        }
    };
}
