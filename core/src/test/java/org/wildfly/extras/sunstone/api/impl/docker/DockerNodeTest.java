package org.wildfly.extras.sunstone.api.impl.docker;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.PortOpeningTimeoutException;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.Constants;

import com.google.common.collect.ImmutableMap;

/**
 * Tests {@link DockerNodeTest} implementation.
 *
 */
public class DockerNodeTest {

    protected static final File WORK_DIR = new File("DockerNodeTest-workdir");

    private static DockerCloudProvider dockerProvider;
    private static DockerNode alpineNode;
    private static DockerNode alpineSshNode;
    private static DockerNode bridgedSshNode;

    /**
     * Configure test properties and create a tested node.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(DockerNodeTest.class);
        dockerProvider = (DockerCloudProvider) CloudProvider.create("provider0");
        alpineNode = (DockerNode) dockerProvider.createNode("alpine");
        alpineNode
                .exec("/bin/bash", "-c",
                        "mkdir -p /opt/{a,b,c}/{aa,bb,cc} && touch /opt/{a,b,c}/{aa,bb,cc}/hello.world "
                                + "&& mkdir -p /opt/d/empty "
                                + "&& touch /opt/{a,b,c}/{aa,bb,cc}/World,\\ Hello! && echo -n Ahoj > /opt/hello");
        alpineSshNode = (DockerNode) dockerProvider.createNode("alpine-ssh");
        bridgedSshNode = (DockerNode) dockerProvider.createNode("bridged");
    }

    /**
     * Reset CloudProperties to default
     *
     * @throws IOException
     */
    @AfterClass
    public static void tearDownClass() throws IOException {
        FileUtils.deleteDirectory(WORK_DIR);
        CloudProperties.getInstance().reset();
        bridgedSshNode.close();
        alpineSshNode.close();
        alpineNode.close();
        dockerProvider.close();
    }

    @Before
    public void setUp() throws IOException {
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(WORK_DIR);
    }

    @Test
    public void testGetName() {
        assertEquals("alpine", alpineNode.getName());
    }

    @Test
    public void testGetImageName() {
        assertEquals("kwart/alpine-ext:3.2-bash", alpineNode.getImageName());
    }

    @Test
    public void testGetCloudProvider() {
        assertEquals(dockerProvider, alpineNode.getCloudProvider());
    }

    /**
     * Tests if returning public address works.
     *
     * @throws MalformedURLException
     */
    @Test
    public void testGetPublicAddress() throws MalformedURLException {
        final String endpoint = dockerProvider.getObjectProperties().getProperty(Config.CloudProvider.Docker.ENDPOINT);
        final URL url = new URL(endpoint);
        assertEquals(url.getHost(), alpineNode.getPublicAddress());
    }

    /**
     * Tests if port-mapping works.
     * @throws IOException
     */
    @Test
    public void testGetPublicTcpPort() throws IOException {
        assertEquals(8822,alpineNode.getPublicTcpPort(8822));
        assertEquals(22,alpineNode.getPublicTcpPort(22));

        assertEquals(-1,bridgedSshNode.getPublicTcpPort(8080));
        assertEquals(19922,bridgedSshNode.getPublicTcpPort(9922));
        try (SocketChannel sch = SocketChannel.open(new InetSocketAddress(bridgedSshNode.getPublicAddress(), 19922))) {
            //OK
        }
        try (SocketChannel sch = SocketChannel.open(new InetSocketAddress(bridgedSshNode.getPublicAddress(), 9922))) {
            fail("Port 9922 should not be open on docker host.");
        } catch (IOException e) {
            //OK
        }

    }

    /**
     * Tests if isPortOpen works as expected.
     */
    @Test
    public void testIsPortOpen() {
        // there is a problem with networkMode==host - alpineNode.isPortOpen(8822) returns true, because the port is opened by
        // second container (alpineSshNode)
        assertFalse(alpineNode.isPortOpen(18080));
        assertTrue(alpineSshNode.isPortOpen(8822));
    }

    /**
     * Tests start/stop/kill and status checking trough isRunning methods.
     *
     * @throws InterruptedException
     */
    @Test
    public void testStartStopKillIsRunning() throws InterruptedException {
        assertTrue(alpineNode.isRunning());
        try (CloudProvider cloudProvider = CloudProvider.create("provider0")) {
            try (Node tmpNode = cloudProvider.createNode("alpine-top",
                    ImmutableMap.<String, String> of("docker.cmd", "sh,-c,sleep 2 && date"))) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                assertFalse(tmpNode.isRunning());
            }
            try (Node tmpNode = cloudProvider.createNode("alpine-top")) {
                assertTrue(tmpNode.isRunning());
                tmpNode.stop();
                assertFalse(tmpNode.isRunning());
                tmpNode.start();
                assertTrue(tmpNode.isRunning());
                tmpNode.kill();
                assertFalse(tmpNode.isRunning());
                tmpNode.start();
                assertTrue(tmpNode.isRunning());

                tmpNode.stop();
                assertFalse(tmpNode.isRunning());
                tmpNode.start();
                assertTrue(tmpNode.isRunning());
                tmpNode.kill();
                assertFalse(tmpNode.isRunning());
                tmpNode.start();
                assertTrue(tmpNode.isRunning());
            }
        }
    }

    /**
     * Tests {@link DockerNode#exec(String...)} call and returned {@link ExecResult} content.
     */
    @Test
    public void testExec() {
        ExecResult result = alpineNode
                .exec("sh", "-c", "echo -n 'this is out' && echo -n 'and this is err' >&2 && exit 2");
        assertNotNull("ExecResult must be not-null", result);
        assertEquals("this is out", result.getOutput());
        assertEquals("and this is err", result.getError());
        assertEquals(2, result.getExitCode());
    }

    /**
     * Tests {@link DockerNode#exec(String...)} call in which executable is not found.
     */
    @Test
    public void testExecNoExecutable() {
        try {
            ExecResult result = alpineNode.exec("sh-c", "echo Ahoj");
            // if the operation completes successfully, then make some tests
            assertNotNull("ExecResult must be not-null", result);
            assertNotEquals("Exit code should not be zero, when command is not found", 0, result.getExitCode());
        } catch (RuntimeException re) {
            assertTrue("Only EOFException is expected/allowed during failed dockerNode.exec() call",
                    re.getCause() instanceof EOFException);
        }
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file throws
     * {@link IllegalArgumentException} when SSH server is not running in a container.
     *
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCopyToNodeWithoutSsh() throws IOException, IllegalArgumentException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeWithoutSsh");
        FileUtils.write(srcFile, "original content");
        alpineNode.copyFileToNode(srcFile.toPath(), "/tmp/testCopyToNodeWithoutSsh");
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for a folder throws
     * {@link IllegalArgumentException}.
     *
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCopyToNodeSrcFolder() throws IOException, IllegalArgumentException, InterruptedException {
        File tmpFile = new File(WORK_DIR, "testCopyToNodeSrcFolder");
        FileUtils.write(tmpFile, "original content");
        alpineSshNode.copyFileToNode(WORK_DIR.toPath(), "/tmp");
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file works when SSH server is running in
     * container.
     *
     * @throws IOException
     */
    @Test
    public void testCopyToNodeFileToNotNewFile() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "src.file");
        FileUtils.write(srcFile, "original content");
        alpineSshNode.copyFileToNode(srcFile.toPath(), "/tmp/test.target");
        assertEquals("original content", alpineSshNode.exec("cat", "/tmp/test.target").getOutput());
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} when target is existing folder.
     *
     * @throws IOException
     */
    @Test
    public void testCopyToNodeFileToExistingFolder() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeFileToExistingFolder");
        FileUtils.write(srcFile, "original content");
        alpineSshNode.copyFileToNode(srcFile.toPath(), "/tmp");
        assertEquals("original content",
                alpineSshNode.exec("cat", "/tmp/testCopyToNodeFileToExistingFolder").getOutput());
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file throws
     * {@link org.jclouds.ssh.SshException} when target folder doesn't exist
     *
     * @throws IOException
     * @throws org.jclouds.ssh.SshException
     */
    @Test(expected = org.jclouds.ssh.SshException.class)
    public void testCopyFileToNodeNotExistingFolder() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "src.file");
        FileUtils.write(srcFile, "original content");
        alpineSshNode.copyFileToNode(srcFile.toPath(), "/nonexisting/test/target");
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file copies to working directory when
     * remoteTarget is null.
     *
     * @throws IOException
     */
    @Test
    public void testCopyToNodeFileToNullTarget() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeFileToNullTarget");
        FileUtils.write(srcFile, "original content");
        alpineSshNode.copyFileToNode(srcFile.toPath(), null);
        assertEquals("original content",
                alpineSshNode.exec("cat", "testCopyToNodeFileToNullTarget").getOutput());
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file copies to working directory when
     * remoteTarget is empty.
     *
     * @throws IOException
     */
    @Test
    public void testCopyToNodeFileToEmptyTarget() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeFileToEmptyTarget");
        FileUtils.write(srcFile, "original content");
        alpineSshNode.copyFileToNode(srcFile.toPath(), null);
        assertEquals("original content",
                alpineSshNode.exec("cat", "testCopyToNodeFileToEmptyTarget").getOutput());
    }

    /**
     * Tests that the calling {@link Node#copyFileToNode(Path, String)} for single file is able to overwrite existing file.
     *
     * @throws IOException
     */
    @Test
    public void testCopyToNodeFileOverwrite() throws IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeFileOverwrite");
        FileUtils.write(srcFile, "original content");
        final String targetFile = "/tmp/testCopyToNodeFileOverwrite";
        alpineSshNode.exec("touch", targetFile);
        alpineSshNode.copyFileToNode(srcFile.toPath(), targetFile);
        assertEquals("original content", alpineSshNode.exec("cat", targetFile).getOutput());
    }

    /**
     * Tests if {@link java.io.FileNotFoundException} is thrown when source file doesn't exist.
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     */
    @Test(expected = FileNotFoundException.class)
    public void testCopyToNodeMissingSrc() throws IllegalArgumentException, IOException, InterruptedException {
        File srcFile = new File(WORK_DIR, "testCopyToNodeMissingSrc");
        alpineSshNode.copyFileToNode(srcFile.toPath(), "/tmp/testCopyToNodeFileToEmptyTarget");
    }

    /**
     * Tests if {@link java.io.FileNotFoundException} is thrown when remote file doesn't exist.
     *
     * @throws IOException
     */
    @Test(expected = FileNotFoundException.class)
    public void testCopyFromNodeWhenRemoteIsMissing() throws IOException {
        alpineNode.copyFileFromNode("/opt/notFound", WORK_DIR.toPath());
    }

    /**
     * Tests copying remote file to existing local file. Overwriting is expected.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFileToFile() throws IOException {
        File targetFile = new File(WORK_DIR, "target.file");
        FileUtils.write(targetFile, "original content");
        assertEquals("Single file expected in the working directory", 1, WORK_DIR.list().length);
        alpineNode.copyFileFromNode("/opt/hello", targetFile.toPath());
        assertEquals("Single file expected in the working directory", 1, WORK_DIR.list().length);
        assertTrue("Target file should be regular file after the copy", targetFile.isFile());
        assertEquals("Ahoj", FileUtils.readFileToString(targetFile, "UTF-8"));
    }

    /**
     * Tests copying remote file to existing local folder. The original name is expected.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFileToFolder() throws IOException {
        File targetFile = new File(WORK_DIR, "hello");
        assertEquals("No file expected in the working directory", 0, WORK_DIR.list().length);
        alpineNode.copyFileFromNode("/opt/hello", WORK_DIR.toPath());
        assertEquals("Single file expected in the working directory", 1, WORK_DIR.list().length);
        assertTrue("Target file should be regular file after the copy", targetFile.isFile());
        assertEquals("Ahoj", FileUtils.readFileToString(targetFile, "UTF-8"));
    }

    /**
     * Tests copying remote file to local path which doesn't exist. Missing parent dirs should be created and file should be
     * renamed.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFileToMissingPath() throws IOException {
        File targetFile = new File(WORK_DIR, "subfolder/target.file");
        assertEquals("No file expected in the working directory", 0, WORK_DIR.list().length);
        alpineNode.copyFileFromNode("/opt/hello", targetFile.toPath());
        assertEquals("Single folder expected in the working directory", 1, WORK_DIR.list().length);
        assertTrue("Target file should be regular file after the copy", targetFile.isFile());
        assertEquals("Ahoj", FileUtils.readFileToString(targetFile, "UTF-8"));
    }

    /**
     * Tests copying remote folder to local existing file. The {@link IllegalArgumentException} is expected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCopyFromNodeFolderToFile() throws IOException {
        File targetFile = new File(WORK_DIR, "target.file");
        FileUtils.write(targetFile, "original content");
        alpineNode.copyFileFromNode("/opt/a/aa", targetFile.toPath());
    }

    /**
     * Tests copying remote folder to local existing folder. Adding the new folder as a subfolder of the existing one is
     * expected.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFolderToFolder() throws IOException {
        File targetFile = new File(WORK_DIR, "opt/hello");
        assertEquals("No file expected in the working directory", 0, WORK_DIR.list().length);
        alpineNode.copyFileFromNode("/opt", WORK_DIR.toPath());
        final File[] listFiles = WORK_DIR.listFiles();
        assertEquals("Single file expected in the working directory", 1, listFiles.length);
        assertTrue("Subdirectory opt should exist in the target location",
                listFiles[0].isDirectory() && "opt".equals(listFiles[0].getName()));
        assertEquals("Ahoj", FileUtils.readFileToString(targetFile, "UTF-8"));
        assertTrue("Target file should be regular file after the copy", targetFile.isFile());

        assertEquals("Folder 'opt/b' should contain 3 subfolders", 3, new File(WORK_DIR, "opt/b").list().length);
        assertEquals("Folder 'opt/b/bb' should contain 3 files", 2, new File(WORK_DIR, "opt/b/bb").list().length);

        assertFileExistsInWorkDir("opt/a/aa/hello.world");
        assertFileExistsInWorkDir("opt/a/cc/World, Hello!");
        assertFileExistsInWorkDir("opt/c/bb/hello.world");
    }

    /**
     * Test overwriting content during copying folder from node.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFolderToFolderWithExistingSubFolder() throws IOException {
        File targetFile = new File(WORK_DIR, "opt/hello");
        targetFile.getParentFile().mkdirs();
        FileUtils.write(targetFile, "original content");

        alpineNode.copyFileFromNode("/opt", WORK_DIR.toPath());
        assertEquals("Ahoj", FileUtils.readFileToString(targetFile, "UTF-8"));
        assertFileExistsInWorkDir("opt/c/aa/hello.world");
        assertFileExistsInWorkDir("opt/c/bb/World, Hello!");

        final File emptyDir = new File(WORK_DIR, "opt/d/empty");
        assertTrue(emptyDir.isDirectory());
        assertEquals(0, emptyDir.list().length);
    }

    /**
     * Tests copying remote folder to local path which doesn't exist. Missing parent dirs should be created and folder should be
     * renamed.
     *
     * @throws IOException
     */
    @Test
    public void testCopyFromNodeFolderToMissingPath() throws IOException {
        File targetFolder = new File(WORK_DIR, "folder/subfolder");
        alpineNode.copyFileFromNode("/opt/a", targetFolder.toPath());
        assertEquals("Single file expected in the working directory", 1, WORK_DIR.list().length);
        assertFileExistsInWorkDir("folder/subfolder/aa/hello.world");
        assertFileExistsInWorkDir("folder/subfolder/cc/World, Hello!");
    }

    /**
     * Tests if {@link NodeMetadata} JClouds implementation is correctly returned.
     */
    @Test
    public void testGetJCloudsImpl() {
        NodeMetadata nodeMetadata = alpineNode.getInitialNodeMetadata();
        assertNotNull(nodeMetadata);
        assertEquals("JClouds Node group name doesn't match the expected value", Constants.JCLOUDS_NODEGROUP,
                nodeMetadata.getGroup());
    }

    @Test
    public void testWaitForPorts() {
        try {
            alpineSshNode.waitForPorts(0, 8822);
        } catch (PortOpeningTimeoutException e) {
            fail("Port 8822 should be open");
        }
        try {
            alpineSshNode.waitForPorts(1, 8822);
        } catch (PortOpeningTimeoutException e) {
            fail("Port 8822 should be open");
        }
        try {
            alpineSshNode.waitForPorts(0, 8823);
            fail("Port 8823 should not be open");
        } catch (PortOpeningTimeoutException e) {
            assertEquals(8823, e.getPortNumber());
        }

    }

    @Test
    public void testIptablesUnprivileged() {
        assertNotEquals(0, bridgedSshNode.exec("sudo", "iptables", "-L").getExitCode());
    }

    @Test
    public void testIptablesPrivileged() {
        try (DockerNode privilegedNode = (DockerNode) dockerProvider.createNode("privileged")) {
            assertEquals(0, privilegedNode.exec("sudo", "iptables", "-L").getExitCode());
        }
    }

    @Test
    public void testIptablesCapabilities() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("capabilities")) {
            assertEquals(0, node.exec("sudo", "iptables", "-L").getExitCode());
        }
    }

    @Test
    public void testInsufficientCapabilities() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("capabilities",
                ImmutableMap.<String, String> of("docker.capAdd", "SYSLOG"))) {
            assertNotEquals(0, node.exec("sudo", "iptables", "-L").getExitCode());
        }
    }

    @Test
    public void testVolumeBindings() {
        final int nonce = new Random().nextInt();
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpineVolumes",
                ImmutableMap.<String, String> of("template", "alpine", "docker.volumeBindings", "/tmp:/data"))) {
            node.exec("sh", "-c", "echo " + nonce + " > /data/testVolumeBindings").assertSuccess();
        }
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpineVolumes",
                ImmutableMap.<String, String> of("template", "alpine", "docker.volumeBindings", "/tmp:/mnt"))) {
            final ExecResult execResult = node.exec("cat", "/mnt/testVolumeBindings");
            execResult.assertSuccess();
            assertThat(execResult.getOutput(), containsString(String.valueOf(nonce)));
            node.exec("rm", "-f", "/mnt/testVolumeBindings").assertSuccess();
        }
    }

    /**
     * Asserts that a regular file described by given path exists in the {@link #WORK_DIR} folder.
     *
     * @param path
     */
    private void assertFileExistsInWorkDir(String path) {
        File targetFile = new File(WORK_DIR, path);
        assertTrue("File " + targetFile + " should exist", targetFile.isFile());
    }

    @Test
    public void testGetProperty() {
        assertEquals("kwart/alpine-ext:3.2-bash", alpineNode.getProperty(Config.Node.Docker.IMAGE, "foobar"));
        assertEquals("kwart/alpine-ext:3.2-bash", alpineNode.config().getProperty(Config.Node.Docker.IMAGE, "foobar"));

        assertEquals("foobar", alpineNode.getProperty("nonexisting.property", "foobar"));
        assertEquals("foobar", alpineNode.config().getProperty("nonexisting.property", "foobar"));
    }
}
