package org.wildfly.extras.sunstone.api.wildfly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

/**
 * Tests functionality of the {@link WildFlyNode} wrapper.
 *
 */
public class WildFlyNodeTest {

    private static CloudProvider cloudProvider;
    private static WildFlyNode wildFlyNode;

    /**
     * Configure test properties.
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(WildFlyNodeTest.class);
        cloudProvider = CloudProvider.create("provider0");
        wildFlyNode = new WildFlyNode(cloudProvider.createNode("node0"));
        wildFlyNode.waitUntilRunning(0);
    }

    /**
     * Reset CloudProperties to default
     */
    @AfterClass
    public static void tearDownClass() {
        CloudProperties.getInstance().reset();
        if (wildFlyNode != null) {
            wildFlyNode.close();
        }
        if (cloudProvider != null) {
            cloudProvider.close();
        }
    }

    @Test
    public void test() throws IOException, CliException {
        final ObjectProperties op = new ObjectProperties(ObjectType.NODE, "node0");
        assertEquals(op.getPropertyAsInt(WildFlyNodeConfig.MGMT_PORT, 9990), wildFlyNode.getMgmtPort());
        assertEquals(cloudProvider, wildFlyNode.getCloudProvider());
        try (OnlineManagementClient managementClient = wildFlyNode.createManagementClient()) {
            wildFlyNode.waitUntilRunning(0);
            assertNotNull(managementClient);
            ModelNodeResult result = managementClient.execute(":whoami");
            assertTrue(result.isSuccess());
            assertEquals("admin", result.get("result", "identity", "username").asString());
        }
    }

    @Test
    public void testReload() throws IOException, CliException, InterruptedException, TimeoutException {
        try (OnlineManagementClient managementClient = wildFlyNode.createManagementClient()) {
            Administration admin = new Administration(managementClient);
            admin.reload();
            Operations ops = new Operations(managementClient);
            assertEquals("admin", ops.whoami().get("result", "identity", "username").asString());
        }
    }

}
