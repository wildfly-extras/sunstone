package org.wildfly.extras.sunstone.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.sunstone.annotations.InjectCloudProvider;
import org.wildfly.extras.sunstone.annotations.InjectNode;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.wildfly.WildFlyNode;

/**
 * Example which shows how to manually control cloud providers and nodes.
 *
 */
@RunWith(Arquillian.class)
public class ManualControlTest {

    private static final String SUITE_LEVEL_NODE = "suite-level-2";


    @ArquillianResource
    @InjectCloudProvider(value = "provider0")
    static CloudProvider cloudProvider;


    @InjectNode(SUITE_LEVEL_NODE)
    @ArquillianResource
    private Node suiteNode;

    /**
     * Configure test properties.
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(ManualControlTest.class);
    }

    /**
     * Reset CloudProperties to default
     */
    @AfterClass
    public static void tearDownClass() {
        CloudProperties.getInstance().reset();
    }

    public static WebArchive createTestDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war").addAsWebResource(new StringAsset("test"), "index.html");
    }

    /**
     * Create node
     *
     * @throws CliException
     */
    @Test
    public void test() throws IOException, CliException {
        assertNotNull(cloudProvider);
        try (WildFlyNode wildFlyNode = new WildFlyNode(cloudProvider.createNode("node0"))) {
            // the 8080 port should be open already, let's wait for the management port too (so 10s should be enough)
            wildFlyNode.waitUntilRunning(10);
            File tempDeploymentFile = File.createTempFile("test-", ".war");
            try (final OnlineManagementClient client = wildFlyNode.createManagementClient()) {
                Operations ops = new Operations(client);
                assertEquals("admin", ops.whoami().get("result", "identity", "username").asString());
                assertTrue(wildFlyNode.isRunning());

                createTestDeployment().as(org.jboss.shrinkwrap.api.exporter.ZipExporter.class).exportTo(tempDeploymentFile,
                        true);

                client.executeCli("deploy --name=test.war " + tempDeploymentFile.getAbsolutePath());
                final Request request = new Request.Builder().url("http://" + wildFlyNode.getPublicAddress() + ":8080/test/")
                        .build();
                assertEquals("test", new OkHttpClient().newCall(request).execute().body().string());
            } finally {
                tempDeploymentFile.delete();
            }
        }
    }


    @Test
    public void testInjectedSuiteLevelNode() throws IOException {
        assertNotNull(suiteNode);
        assertEquals(SUITE_LEVEL_NODE, suiteNode.getName());
    }
}
