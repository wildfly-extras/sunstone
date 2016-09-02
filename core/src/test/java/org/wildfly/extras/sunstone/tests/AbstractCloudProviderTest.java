package org.wildfly.extras.sunstone.tests;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.CreatedNodes;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.PortOpeningException;
import org.wildfly.extras.sunstone.api.process.ExecBuilder;

/**
 * Subclasses must be acompanied by a {@code .properties} classpath resource that defines the tested cloud provider.
 * The {@code .properties} file must contain a definition of:
 *
 * <ul>
 *     <li>a cloud provider called {@code myprovider}</li>
 *     <li>a node called {@code mynode}</li>
 *     <li>another node called {@code othernode}</li>
 *     <li>nothing else</li>
 * </ul>
 *
 * The node {@code mynode} is expected to run Linux and expose SSH on port returned
 * by {@link TestedCloudProvider#getPrivateSshPort(Node)}. The user that will be used for SSH
 * must not be {@code root}, but {@code sudo} without password must be possible. The node must not expose port
 * {@code 54321}. If the node has port mapping, it must not have port mapping for port {@code 54321}.
 * The node must not have any {@code bootScript} property specified (the test will provide one doing {@code touch /tmp/bootScript.test}).
 * There are no particular requirements for node {@code othernode}, it can be absolutely minimal.
 */
// TODO WildFly node? (probably require a new node "wildflynode" in cloud.properties, sounds easiest...)
public abstract class AbstractCloudProviderTest {
    private final TestedCloudProvider testedCloudProvider;

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    protected AbstractCloudProviderTest(TestedCloudProvider testedCloudProvider) {
        this.testedCloudProvider = testedCloudProvider;
    }

    @Before
    public final void setUp() throws IOException {
        CloudProperties.getInstance().reset().load(this.getClass());
    }

    @After
    public final void tearDown() {
        CloudProperties.getInstance().reset();
    }

    @Test
    public final void cloudProvider() throws OperationNotSupportedException {
        try {
            CloudProvider.create(null);
            fail("NullPointerException expected when calling createCloudProvider(null)");
        } catch (NullPointerException ignored) {
            // OK
        }

        try {
            CloudProvider.create("foobar");
            fail("Exception expected when calling createCloudProvider(...) with a nonexisting cloud provider");
        } catch (Exception ignored) {
            // OK
        }

        try (CloudProvider cloudProvider = CloudProvider.create("myprovider")) {
            assertNotNull("Cloud provider 'myprovider' should be created", cloudProvider);
            assertEquals("myprovider", cloudProvider.getName());
            assertEquals(testedCloudProvider.type(), cloudProvider.getCloudProviderType());

            assertNotNull(cloudProvider.getNodes());
            assertEquals(0, cloudProvider.getNodes().size());
            assertNull(cloudProvider.getNode("mynode"));
            assertNull(cloudProvider.getNode("othernode"));
            assertNull(cloudProvider.getNode("foobar"));
            assertNull(cloudProvider.getNode(""));

            try {
                cloudProvider.getNode(null);
                fail("NullPointerException expected when calling getNode(null)");
            } catch (NullPointerException ignored) {
                // OK
            }

            try {
                cloudProvider.getNodes().add(mock(Node.class));
            } catch (Exception ignored) {
                // the list might be immutable, don't really care
                // the point is that the user must not be able to affect the state of the provider
            }
            assertEquals(0, cloudProvider.getNodes().size());
        }

        Map<String, String> overrides = testedCloudProvider.overridesThatPreventCreatingNode();
        if (overrides != null) {
            try (CloudProvider cloudProvider = CloudProvider.create("myprovider", overrides)) {
                cloudProvider.createNode("mynode");
                fail("CloudProvider with these overrides should not be able to create nodes: " + overrides);
            } catch (Exception ignored) {
                // OK
            }
        }
    }

    @Test
    public final void node() throws Exception {
        try (CloudProvider cloudProvider = CloudProvider.create("myprovider")) {
            assertEquals(0, cloudProvider.getNodes().size());

            try {
                cloudProvider.createNode(null);
                fail("NullPointerException expected when calling createNode(null)");
            } catch (NullPointerException ignored) {
                // OK
            }

            try {
                cloudProvider.createNode(null, new HashMap<>());
                fail("NullPointerException expected when calling createNode(null, ...)");
            } catch (NullPointerException ignored) {
                // OK
            }

            try {
                cloudProvider.createNode("foobar");
                fail("Exception expected when calling createNode(...) with a nonexisting node");
            } catch (Exception ignored) {
                // OK
            }

            System.setProperty("node.mynode.bootScript", "touch /tmp/bootScript.test");

            try (CreatedNodes createdNodes = cloudProvider.createNodes("mynode", "othernode")) {
                assertNotNull(createdNodes);
                assertEquals(2, createdNodes.size());

                final Node node = createdNodes.get(0);
                final Node node2 = createdNodes.get(1);
                assertNotNull(node);
                assertEquals("mynode", node.getName());
                assertNotNull(node2);
                assertEquals("othernode", node2.getName());

                assertEquals(2, cloudProvider.getNodes().size());
                assertEquals(node, cloudProvider.getNode("mynode"));
                assertEquals(node2, cloudProvider.getNode("othernode"));
                assertNull(cloudProvider.getNode("MyNode"));

                if (testedCloudProvider.hasImages()) {
                    assertThat(node.getImageName(), is(not(emptyOrNullString())));
                }
                assertEquals(cloudProvider, node.getCloudProvider());
                assertThat(node.getPublicAddress(), is(not(emptyOrNullString())));
                assertThat(node.getPrivateAddress(), is(not(emptyOrNullString())));

                node.exec("test", "-f", "/tmp/bootScript.test").assertSuccess("File /tmp/bootScript.test should exist in the Node.");

                testPorts(node);
                testCommandExecution(node);
                testExecBuilder(node);
                testLifecycleControl(node);
                testFileCopying(node);

                try (Node secondNode = cloudProvider.createNode("mynode")) {
                    fail("Creating second node with existing name shouldn't be allowed");
                } catch (IllegalArgumentException e) {
                    // expected
                }
            }

            assertEquals(0, cloudProvider.getNodes().size());
            assertNull(cloudProvider.getNode("mynode"));
            assertNull(cloudProvider.getNode("othernode"));
        }
    }

    /**
     * Method used to retrieve private (not-mapped) port number on which SSH server in given node was started. The default
     * implementation returns {@code 22}.
     *
     * @return unmapped SSH port number.
     */
    protected int getPrivateSshPort(Node node) {
        return 22;
    }

    private void testPorts(Node node) {
        if (testedCloudProvider.hasPortMapping()) {
            assertEquals(-1, node.getPublicTcpPort(54321));
        } else {
            assertEquals(22, node.getPublicTcpPort(22));
            assertEquals(54321, node.getPublicTcpPort(54321));
        }

        final int privateSshPort = testedCloudProvider.getPrivateSshPort(node);
        assertTrue(node.isPortOpen(privateSshPort));
        assertFalse(node.isPortOpen(54321));

        try {
            node.waitForPorts(0, privateSshPort);
        } catch (PortOpeningException e) {
            fail("Waiting for port 22 shouldn't fail");
        }
        try {
            node.waitForPorts(1, privateSshPort);
        } catch (PortOpeningException e) {
            fail("Waiting for port 22 shouldn't fail");
        }
        try {
            node.waitForPorts(1, 54321);
            fail("Waiting for port 54312 should fail");
        } catch (PortOpeningException expected) {
            assertEquals("Port expected in Exception details is 54321", 54321 , expected.getPortNumber());
        }
    }

    private void testCommandExecution(Node node) throws IOException, InterruptedException {
        if (!testedCloudProvider.commandExecutionSupported()) {
            return;
        }

        // this assumes Linux, which is probably fine

        ExecResult result = node.exec("id");
        assertEquals(0, result.getExitCode());

        assertThat(result.getOutput(), is(not(emptyOrNullString())));
        assertThat(result.getError(), is(emptyOrNullString()));

        result = node.exec("echo", "foobar");
        assertEquals(0, result.getExitCode());
        assertThat(result.getOutput(), containsString("foobar"));
        assertThat(result.getError(), is(emptyOrNullString()));

        result = node.exec("doesnt-exist", "foobar");
        assertNotEquals(0, result.getExitCode());
    }

    private void testExecBuilder(Node node) throws IOException, InterruptedException {
        if (!testedCloudProvider.execBuilderSupported()) {
            return;
        }

        // this assumes Linux, which is probably fine

        ExecResult result = ExecBuilder.fromCommand("id").exec(node);
        assertEquals(0, result.getExitCode());
        assertThat(result.getOutput(), not(containsString("root")));

        result = ExecBuilder.fromCommand("id").withSudo().exec(node);
        assertEquals(0, result.getExitCode());
        assertThat(result.getOutput(), containsString("root"));

        result = ExecBuilder.fromCommand("id").redirectOut("/tmp/my-id.txt").exec(node);
        assertEquals(0, result.getExitCode());
        assertThat(node.exec("cat", "/tmp/my-id.txt").getOutput(), not(containsString("root")));

        result = ExecBuilder.fromCommand("id").withSudo().redirectOut("/tmp/my-sudo-id.txt").exec(node);
        assertEquals(0, result.getExitCode());
        assertThat(node.exec("cat", "/tmp/my-sudo-id.txt").getOutput(), containsString("root"));

        long sleepInSecs = 60;
        assertEquals("No running 'sleep' process should be found", 1, node.exec("pgrep","sleep").getExitCode());
        Instant start = Instant.now();
        result = ExecBuilder.fromCommand("sh", "-c", "sleep " + sleepInSecs).asDaemon().exec(node);
        Instant end = Instant.now();
        assertThat("ExecBuilder.asDaemon().exec() should return immediatelly",
                Duration.between(start, end).getSeconds(), lessThan(sleepInSecs));
        assertSame("ExecBuilder.asDaemon().exec() should return the constant", ExecBuilder.EXEC_RESULT_DAEMON, result);
        assertEquals("Running 'sleep' process should be found", 0, node.exec("pgrep","sleep").getExitCode());
    }

    private void testLifecycleControl(Node node) {
        assertTrue(node.isRunning());

        if (!testedCloudProvider.lifecycleControlSupported()) {
            return;
        }

        assertTrue(node.isRunning());
        node.stop();
        assertFalse(node.isRunning());
        node.start();
        assertTrue(node.isRunning());
        node.kill();
        assertFalse(node.isRunning());
        node.start();
        assertTrue(node.isRunning());
    }

    private void testFileCopying(Node node) throws IOException, InterruptedException {
        if (!testedCloudProvider.fileCopyingSupported()) {
            return;
        }

        // this assumes Linux, which is probably fine

        Path textFile = tmp.getRoot().toPath().resolve("foobar.txt");
        Files.write(textFile, "foo bar".getBytes(StandardCharsets.US_ASCII));

        node.copyFileToNode(textFile, "/tmp/foobar.txt");

        if (testedCloudProvider.commandExecutionSupported()) {
            ExecResult result = node.exec("cat", "/tmp/foobar.txt");
            assertEquals(0, result.getExitCode());
            assertThat(result.getOutput(), containsString("foo bar"));
        }

        Path copiedTextFile = tmp.getRoot().toPath().resolve("copied.txt");
        node.copyFileFromNode("/tmp/foobar.txt", copiedTextFile);

        String content = new String(Files.readAllBytes(copiedTextFile), StandardCharsets.US_ASCII);
        assertThat(content, containsString("foo bar"));
    }
}
