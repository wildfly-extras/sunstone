package org.wildfly.extras.sunstone.api.impl.docker;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.collect.ImmutableMap;

/**
 * Tests {@link DockerCloudProvider} implementation.
 *
 */
public class DockerCloudProviderTest {

    /**
     * Configure test properties.
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(DockerCloudProviderTest.class);
    }

    /**
     * Reset CloudProperties to default
     */
    @AfterClass
    public static void tearDownClass() {
        CloudProperties.getInstance().reset();
    }

    @Test
    public void testCloudProvider() throws OperationNotSupportedException {
        try (CloudProvider cloudProvider = CloudProvider.create("provider1")) {
            assertNotNull("Cloud provider was not created", cloudProvider);
            assertEquals("provider1", cloudProvider.getName());
            assertTrue("Cloud provider is not of expected type", cloudProvider instanceof DockerCloudProvider);
            assertTrue("Cloud provider is not of expected type", cloudProvider instanceof JCloudsCloudProvider);

            @SuppressWarnings("resource")
            JCloudsCloudProvider jCloudsProvider = (JCloudsCloudProvider) cloudProvider;
            Assert.assertThat(cloudProvider.getCloudProviderType(), is(CloudProviderType.DOCKER));

            assertNotNull(jCloudsProvider.getComputeServiceContext());

            try (Node node = cloudProvider.createNode("busybox")) {
                assertNotNull("Null Node returned from createNode method", node);
                assertTrue("Node created by createNode() should be running", node.isRunning());
                assertTrue("Wrong Node type", node instanceof DockerNode);
                assertTrue("Wrong Node type", node instanceof JCloudsNode);
            }
        }
    }

    @Test
    public void testCreateCloudProviderWithOverrides() {
        try (CloudProvider cloudProvider = CloudProvider.create("provider1",
                ImmutableMap.<String, String> of("docker.endpoint", "wrongValueHere"))) {
            cloudProvider.createNode("busybox");
            fail("CloudProvider with wrong endpoint should not be able to create nodes");
        } catch (Exception e) {
            // OK, wrong value was propagated to endpoint settings.
        }
    }

    @Test
    public void testCreateNodeWithOverrides() throws Exception {
        try (CloudProvider cloudProvider = CloudProvider.create("provider1")) {
            try (Node node = cloudProvider.createNode("busybox",
                    ImmutableMap.<String, String> of("docker.cmd", "sh,-c,sleep 2 && echo hello"))) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(4));
                assertFalse("Node should not be running anymore", node.isRunning());
            }
        }
    }

    @Test
    public void testGetNodes() {
        try (CloudProvider cloudProvider = CloudProvider.create("provider1")) {
            assertEquals(0, cloudProvider.getNodes().size());
            try (Node node1 = cloudProvider.createNode("busybox2"); Node node2 = cloudProvider.createNode("busybox3")) {
                assertEquals(2, cloudProvider.getNodes().size());
                assertEquals(cloudProvider.getNode("busybox2"), node1);
                assertNull(cloudProvider.getNode("busybox4"));
            }
            assertEquals(0, cloudProvider.getNodes().size());
        }
    }

    @Test
    public void testDefaultObjectProperties() {
        try (DockerCloudProvider cloudProvider = (DockerCloudProvider) CloudProvider.create("provider2")) {
            ObjectProperties op = cloudProvider.getObjectProperties();
            assertNotNull(op);
            assertEquals(ObjectType.CLOUD_PROVIDER, op.getObjectType());
            assertEquals("docker", op.getProperty("type"));
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNodeObjectProperties() {
        try (DockerCloudProvider cloudProvider = (DockerCloudProvider) CloudProvider.create("provider1")) {
            try (Node node1 = cloudProvider.createNode("nodeWithNoDockerImage")) {
                fail("IllegalArgumentException is expected when creating docker node without specifying docker.image property.");
            }
        }
    }

    @Test
    public void testGetProperty() {
        try (CloudProvider cloudProvider = CloudProvider.create("provider3")) {
            assertEquals("1.18", cloudProvider.getProperty(Config.CloudProvider.Docker.API_VERSION, "foobar"));
            assertEquals("1.18", cloudProvider.config().getProperty(Config.CloudProvider.Docker.API_VERSION, "foobar"));

            assertEquals("foobar", cloudProvider.getProperty("nonexisting.property", "foobar"));
            assertEquals("foobar", cloudProvider.config().getProperty("nonexisting.property", "foobar"));
        }
    }
}
