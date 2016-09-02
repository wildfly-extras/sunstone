package org.wildfly.extras.sunstone.tests.baremetal;

import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.CreatedNodes;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.tests.AbstractCloudProviderTest;
import org.wildfly.extras.sunstone.tests.TestedCloudProvider;

public class BareMetalTest extends AbstractCloudProviderTest {

    private static CloudProvider dockerProvider;
    private static CreatedNodes dockerNodes;

    public BareMetalTest() throws IOException {
        super(BARE_METAL);
    }

    @BeforeClass
    public static void beforeClass() {
        CloudProperties.getInstance().reset().load(BareMetalTest.class);
        dockerProvider = CloudProvider.create("dockerProvider");
        dockerNodes = dockerProvider.createNodes("dockerNode0", "dockerNode1");
        System.setProperty("baremetalHost", dockerNodes.get(0).getPublicAddress());
    }

    @AfterClass
    public static void afterClass() {
        try {
            dockerNodes.close();
        } catch (Exception e) {

            e.printStackTrace();
        }
        dockerProvider.close();
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

        public int getPrivateSshPort(Node node) {
            if ("mynode".equals(node.getName())) {
                return 8822;
            } else {
                return 18822;
            }
        }


    };
}
