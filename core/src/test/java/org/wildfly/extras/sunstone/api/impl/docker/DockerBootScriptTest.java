package org.wildfly.extras.sunstone.api.impl.docker;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;

import com.google.common.collect.ImmutableMap;

/**
 * Tests bootScript and bootScript.file node properties.
 */
public class DockerBootScriptTest {

    private static final String GENERAL_SCRIPT = "bootScript";
    private static final String GENERAL_FILE = "bootScript.file";
    private static final String SPECIFIC_SCRIPT = "docker.bootScript";
    private static final String SPECIFIC_FILE = "docker.bootScript.file";

    private static DockerCloudProvider dockerProvider;

    /**
     * Configure test properties and create a tested node.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(DockerBootScriptTest.class);
        dockerProvider = (DockerCloudProvider) CloudProvider.create("provider0");
    }

    /**
     * Reset CloudProperties to default
     *
     * @throws IOException
     */
    @AfterClass
    public static void tearDownClass() throws IOException {
        dockerProvider.close();
        CloudProperties.getInstance().reset();
    }

    /**
     * Tests an exception comes when both script properties are configured (inline and file) on provider specific level.
     */
    @Test(expected = RuntimeException.class)
    public void testBothProviderSpecific() {
        testInternal(null, SPECIFIC_FILE, SPECIFIC_SCRIPT);
    }

    /**
     * Tests an exception comes when both script properties are configured (inline and file) on shared level.
     */
    @Test(expected = RuntimeException.class)
    public void testBothGeneral() {
        testInternal(null, GENERAL_FILE, GENERAL_SCRIPT);
    }

    /**
     * Tests that provider specific script has higher priority than anything on shared level.
     */
    @Test
    public void testScriptProviderSpecificBothGeneral() {
        testInternal(SPECIFIC_SCRIPT, GENERAL_FILE, GENERAL_SCRIPT, SPECIFIC_SCRIPT);
    }

    /**
     * Tests that provider specific script file has higher priority than anything on shared level.
     */
    @Test
    public void testFileProviderSpecificBothGeneral() {
        testInternal(SPECIFIC_FILE, GENERAL_FILE, GENERAL_SCRIPT, SPECIFIC_FILE);
    }

    /**
     * Tests that shared script file works.
     */
    @Test
    public void testGeneralFileOnly() {
        testInternal(GENERAL_FILE, GENERAL_FILE);
    }

    /**
     * Tests that shared script works.
     */
    @Test
    public void testGeneralScriptOnly() {
        testInternal(GENERAL_SCRIPT, GENERAL_SCRIPT);
    }

    /**
     * Tests that provider specific script works.
     */
    @Test
    public void testSpecificScriptOnly() {
        testInternal(SPECIFIC_SCRIPT, SPECIFIC_SCRIPT);
    }

    /**
     * Tests that provider specific script file works.
     */
    @Test
    public void testSpecificFileOnly() {
        testInternal(SPECIFIC_FILE, SPECIFIC_FILE);
    }

    @Test(expected = RuntimeException.class)
    public void testSpecificFileWrongPath() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of(SPECIFIC_FILE, "classpath:this/path/doesnt/exist"))) {
        }
    }

    @Test
    public void testWithSudo() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of("bootScript", "touch /tmp/`whoami`.test", "bootScript.withSudo", "true"))) {
            node.exec("test", "-f", "/tmp/root.test").assertSuccess();
        }
    }

    @Test
    public void testWithoutSudo() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of("bootScript", "touch /tmp/`whoami`.test", "bootScript.withSudo", "false"))) {
            node.exec("test", "-f", "/tmp/alpine.test").assertSuccess();
        }
    }

    @Test
    public void testDefaultSudo() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of("bootScript", "touch /tmp/`whoami`.test"))) {
            node.exec("test", "-f", "/tmp/root.test").assertSuccess();
        }
    }

    @Test
    public void testCustomSudoCommand() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of("bootScript", "touch /tmp/`whoami`.test", "sudo.command", "time"))) {
            node.exec("test", "-f", "/tmp/alpine.test").assertSuccess();
        }
    }

    @Test
    public void testEmptySudoCommand() {
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh",
                ImmutableMap.of("bootScript", "touch /tmp/`whoami`.test", "sudo.command", ""))) {
            node.exec("test", "-f", "/tmp/alpine.test").assertSuccess();
        }
    }

    /**
     * Starts SSH-able docker Node with values of given bootScript properties configured and checks if the expected one is used
     * (or exception is thrown when {@code expectedProperty == null}).
     *
     * @param expectedProperty
     * @param bootScriptProps
     */
    private void testInternal(String expectedProperty, String... bootScriptProps) {
        Map<String, String> overrides = Arrays.stream(bootScriptProps)
                .collect(Collectors.toMap(s -> s, s -> s.endsWith(".file") ? "classpath:" + s + ".sh" : "touch /tmp/" + s));
        try (DockerNode node = (DockerNode) dockerProvider.createNode("alpine-ssh", overrides)) {
            if (expectedProperty == null) {
                fail("Node creation should fail");
            }
            node.exec("test", "-f", "/tmp/" + expectedProperty).assertSuccess();
        }
    }
}
