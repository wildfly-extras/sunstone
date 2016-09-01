package org.wildfly.extras.sunstone.api.impl.baremetal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.channels.SocketChannel;

import org.jclouds.compute.domain.NodeMetadata;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.PortOpeningTimeoutException;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.Constants;
import org.wildfly.extras.sunstone.api.impl.docker.DockerCloudProvider;
import org.wildfly.extras.sunstone.api.impl.docker.DockerNode;

import com.google.common.collect.ImmutableMap;

/**
 * Tests for {@link BareMetalNode} implementation. It's based on SSH-able Docker container.
 */
public class BareMetalNodeTest {

    private static DockerCloudProvider dockerProvider;
    private static DockerNode dockerNode;

    private static BareMetalCloudProvider bareMetalProvider;
    private static BareMetalNode bareMetalNode;

    /**
     * Configure test properties and create a tested nodes.
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(BareMetalNodeTest.class);
        dockerProvider = (DockerCloudProvider) CloudProvider.create("provider0");
        dockerNode = (DockerNode) dockerProvider.createNode("alpine-ssh");

        // as there is a limitation in the baremetal provider, which requires to define nodes when creating provider, we can't
        // configure these properties through overrides
        System.setProperty("node.alpine-ssh.baremetal.host", dockerNode.getPublicAddress());
        System.setProperty("node.alpine-ssh.baremetal.privateAddress", dockerNode.getPrivateAddress());
        bareMetalProvider = (BareMetalCloudProvider) CloudProvider.create("provider0", ImmutableMap.of("type", "baremetal"));
        bareMetalNode = (BareMetalNode) bareMetalProvider.createNode("alpine-ssh");
    }

    /**
     * Reset CloudProperties to default
     */
    @AfterClass
    public static void tearDownClass() throws IOException {
        bareMetalNode.close();
        bareMetalProvider.close();
        dockerNode.close();
        dockerProvider.close();
        CloudProperties.getInstance().reset();
    }

    @Test
    public void testGetName() {
        assertEquals("alpine-ssh", bareMetalNode.getName());
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testGetImageName() {
        bareMetalNode.getImageName();
    }

    @Test
    public void testGetCloudProvider() {
        assertEquals(bareMetalProvider, bareMetalNode.getCloudProvider());
    }

    /**
     * Tests if returning public address works.
     */
    @Test
    public void testGetPublicAddress() throws MalformedURLException {
        assertEquals(dockerNode.getPublicAddress(), bareMetalNode.getPublicAddress());
    }

    @Test
    public void testGetPublicTcpPort() throws IOException {
        assertEquals(8822, bareMetalNode.getPublicTcpPort(8822));
        assertEquals(22, bareMetalNode.getPublicTcpPort(22));
        assertEquals(8080, bareMetalNode.getPublicTcpPort(8080));

        try (SocketChannel sch = SocketChannel.open(new InetSocketAddress(bareMetalNode.getPublicAddress(), 8822))) {
            // OK
        }
    }

    /**
     * Tests if isPortOpen works as expected.
     */
    @Test
    public void testIsPortOpen() {
        assertFalse(bareMetalNode.isPortOpen(18080));
        assertTrue(bareMetalNode.isPortOpen(8822));
    }

    /**
     * Tests {@link DockerNode#exec(String...)} call and returned {@link ExecResult} content.
     */
    @Test
    public void testExec() throws OperationNotSupportedException, IOException, InterruptedException {
        ExecResult result = bareMetalNode.exec("sh", "-c", "echo -n 'this is out' && echo -n 'and this is err' >&2 && exit 2");
        assertNotNull("ExecResult must be not-null", result);
        assertEquals("this is out", result.getOutput());
        assertEquals("and this is err", result.getError());
        assertEquals(2, result.getExitCode());
    }

    @Test
    public void testGetPrivateAddress() {
        final String privateAddress = bareMetalNode.getPrivateAddress();
        assertNotNull(privateAddress);
        assertNotEquals("Private and public address should be different", privateAddress, bareMetalNode.getPublicAddress());
        assertEquals(dockerNode.getPrivateAddress(), privateAddress);
    }


    @Test
    public void testExecNoExecutable() throws IOException, InterruptedException {
        bareMetalNode.exec("sh-c", "echo Ahoj").assertFailure();
    }

    /**
     * Tests if {@link NodeMetadata} JClouds implementation is correctly returned.
     */
    @Test
    public void testGetJCloudsImpl() {
        NodeMetadata nodeMetadata = bareMetalNode.getInitialNodeMetadata();
        assertNotNull(nodeMetadata);
        assertEquals("JClouds Node group name doesn't match the expected value", Constants.JCLOUDS_NODEGROUP,
                nodeMetadata.getGroup());
    }

    @Test
    public void testWaitForPorts() {
        try {
            bareMetalNode.waitForPorts(0, 8822);
        } catch (PortOpeningTimeoutException e) {
            fail("Port 8822 should be open");
        }
        try {
            bareMetalNode.waitForPorts(1, 8822);
        } catch (PortOpeningTimeoutException e) {
            fail("Port 8822 should be open");
        }
        try {
            bareMetalNode.waitForPorts(0, 8823);
            fail("Port 8823 should not be open");
        } catch (PortOpeningTimeoutException e) {
            assertEquals(8823, e.getPortNumber());
        }

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetProperty() {
        assertEquals("8822", bareMetalNode.getProperty(Config.Node.BareMetal.SSH_PORT, "foobar"));
        assertEquals("8822", bareMetalNode.config().getProperty(Config.Node.BareMetal.SSH_PORT, "foobar"));

        assertEquals("foobar", bareMetalNode.getProperty("nonexisting.property", "foobar"));
        assertEquals("foobar", bareMetalNode.config().getProperty("nonexisting.property", "foobar"));
    }

}
