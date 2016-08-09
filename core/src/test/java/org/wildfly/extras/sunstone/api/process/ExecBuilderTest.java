package org.wildfly.extras.sunstone.api.process;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.NodeWrapper;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.impl.docker.DockerCloudProvider;
import org.wildfly.extras.sunstone.api.process.ExecBuilder.RedirectMode;

import com.google.common.collect.ImmutableMap;

/**
 * Tests {@link ExecBuilderTest} implementation.
 *
 */
public class ExecBuilderTest {

    private static final String NODENAME = "ssh";

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private static DockerCloudProvider dockerProvider;

    @BeforeClass
    public static void setUpClass() throws IOException {
        CloudProperties.getInstance().reset().load(ExecBuilderTest.class);
        dockerProvider = (DockerCloudProvider) CloudProvider.create("provider0");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        CloudProperties.getInstance().reset();
        dockerProvider.close();
    }

    /**
     * Tests executing commands on background (as daemons).
     */
    @Test
    public void testExecAsDaemon() throws IOException, InterruptedException {
        final int sleepInSecs = 15;
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final ExecBuilder sleepCheckExec = ExecBuilder.fromCommand("pgrep", "sleep");

            assertNotEquals("Unexpected 'sleep' process running found", 0, sleepCheckExec.exec(sshNode).getExitCode());

            Instant start = Instant.now();
            ExecResult result = ExecBuilder.fromCommand("sh", "-c", "sleep " + sleepInSecs).asDaemon().exec(sshNode);
            Instant end = Instant.now();
            assertTrue("Daemon execution should finish immediatelly", Duration.between(start, end).getSeconds() < sleepInSecs);
            assertSame("Unexpected ExecResult returned from daemon process run", ExecBuilder.EXEC_RESULT_DAEMON, result);

            assertEquals("Running 'sleep' process was expected in the container, it was not found in the container.", 0,
                    sleepCheckExec.exec(sshNode).getExitCode());
        }
    }

    /**
     * Tests executing commands with sudo.
     */
    @Test
    public void testExecWithSudo() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final ExecBuilder execBuilder = ExecBuilder.fromCommand("whoami");

            ExecResult execResult = execBuilder.exec(sshNode);
            assertEquals("Unexpected 'whoami' exit code", 0, execResult.getExitCode());
            assertThat(execResult.getOutput(), containsString("alpine"));

            execResult = execBuilder.withSudo().exec(sshNode);
            assertEquals("Unexpected 'whoami' exit code", 0, execResult.getExitCode());
            assertThat(execResult.getOutput(), containsString("root"));

            final String redirectOutFile = "/etc/whoamiTest";
            ExecBuilder.fromCommand("whoami").asDaemon().withSudo().redirectOut(redirectOutFile).exec(sshNode);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Path localTarget = tmp.getRoot().toPath().resolve("whoamiTest");
            sshNode.copyFileFromNode(redirectOutFile, localTarget);
            final String fileContent = new String(Files.readAllBytes(localTarget), StandardCharsets.US_ASCII);
            assertThat(fileContent, startsWith("root"));
        }
    }

    /**
     * Test exit codes returned from {@link ExecBuilder#exec(Node)} in {@link ExecResult}.
     */
    @Test
    public void testExitCodes() throws IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            assertEquals(2, ExecBuilder.fromCommand("exit", "2").exec(sshNode).getExitCode());
            assertEquals(3, ExecBuilder.fromCommand("exit", "3").withSudo().exec(sshNode).getExitCode());
            final int daemonExitCode = ExecBuilder.EXEC_RESULT_DAEMON.getExitCode();
            assertEquals(daemonExitCode, ExecBuilder.fromCommand("exit", "4").asDaemon().exec(sshNode).getExitCode());
            assertEquals(daemonExitCode,
                    ExecBuilder.fromCommand("exit", "5").asDaemon().withSudo().exec(sshNode).getExitCode());

            ExecResult result = ExecBuilder.fromCommand("sed", "#s#umask 022#umask 002", "-i", "/etc/profile").withSudo()
                    .exec(sshNode);
            assertEquals(0, result.getExitCode());
        }
    }

    @Test
    public void testExecWrapped() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final ExecBuilder execBuilder = ExecBuilder.fromCommand("whoami");
            Node firstWrap = new NodeWrapper(sshNode);
            Node secondWrap = new NodeWrapper(firstWrap);
            assertThat(execBuilder.exec(firstWrap).getOutput(), containsString("alpine"));
            assertThat(execBuilder.exec(secondWrap).getOutput(), containsString("alpine"));
        }
    }

    @Test
    public void testAssertions() throws IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            ExecBuilder.fromCommand("exit", "0").exec(sshNode).assertSuccess();
            ExecBuilder.fromCommand("exit", "1").exec(sshNode).assertFailure();

            ExecBuilder.fromCommand("echo", "foobar").exec(sshNode).assertOutputContains("foobar");
            ExecBuilder.fromCommand("echo", "foobar").exec(sshNode).assertOutputDoesntContain("quux");

            ExecResult daemonResult = ExecBuilder.fromCommand("sleep", "1").asDaemon().exec(sshNode);
            try {
                daemonResult.assertSuccess();
                throw new RuntimeException();
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("daemon"));
            }

            try {
                daemonResult.assertOutputContains("");
                throw new RuntimeException();
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("daemon"));
            }
        }
    }

    /**
     * Tests redirecting output and error streams.
     */
    @Test
    public void testRedirects() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final String rOut = "/tmp/redirect.out";
            final String rErr = "/tmp/redirect.err";

            ExecBuilder execBuilder = ExecBuilder.fromCommand("whoami").redirectOut(rOut, RedirectMode.APPEND);
            assertEquals(0, execBuilder.exec(sshNode).getExitCode());
            assertEquals("alpine\n", sshNode.exec("cat", rOut).getOutput());
            assertEquals(0, execBuilder.exec(sshNode).getExitCode());
            assertEquals("alpine\nalpine\n", sshNode.exec("cat", rOut).getOutput());

            ExecBuilder.fromCommand("whoami").redirectOut(rOut).exec(sshNode);
            assertEquals("alpine\n", sshNode.exec("cat", rOut).getOutput());

            execBuilder = ExecBuilder.fromCommand("sh","-c", "whoami >&2").redirectErr(rErr, RedirectMode.APPEND);
            assertEquals(0, execBuilder.exec(sshNode).getExitCode());
            assertEquals("alpine\n", sshNode.exec("cat", rErr).getOutput());
            assertEquals(0, execBuilder.exec(sshNode).getExitCode());
            assertEquals("alpine\nalpine\n", sshNode.exec("cat", rErr).getOutput());

            ExecBuilder.fromCommand("sh","-c", "whoami >&2").redirectErr(rErr).exec(sshNode);
            assertEquals("alpine\n", sshNode.exec("cat", rErr).getOutput());
        }
    }

    /**
     * Tests redirecting output and error when command is executed as a daemon.
     */
    @Test
    public void testRedirectsAsDaemon() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final String rOut = "/tmp/redirect.out";
            final String rErr = "/tmp/redirect.err";

            ExecBuilder execBuilder = ExecBuilder.fromCommand("whoami").asDaemon().redirectOut(rOut, RedirectMode.APPEND);
            execBuilder.exec(sshNode);
            execBuilder.exec(sshNode);

            execBuilder = ExecBuilder.fromCommand("sh","-c", "whoami >&2").asDaemon().redirectErr(rErr, RedirectMode.APPEND);
            execBuilder.exec(sshNode);
            execBuilder.exec(sshNode);

            Thread.sleep(1000L);
            assertEquals("alpine\nalpine\n", sshNode.exec("cat", rOut).getOutput());
            assertEquals("alpine\nalpine\n", sshNode.exec("cat", rErr).getOutput());

            ExecBuilder.fromCommand("whoami").asDaemon().redirectOut(rOut).exec(sshNode);
            ExecBuilder.fromCommand("sh","-c", "whoami >&2").asDaemon().redirectErr(rErr).exec(sshNode);

            Thread.sleep(1000L);
            assertEquals("alpine\n", sshNode.exec("cat", rErr).getOutput());
            assertEquals("alpine\n", sshNode.exec("cat", rOut).getOutput());
        }
    }

    @Test
    public void testFromShellScript() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            final String rErr = "/tmp/redirect.err";
            ExecBuilder execBuilder = ExecBuilder.fromShellScript("whoami >&2").redirectErr(rErr, RedirectMode.APPEND);
            assertEquals(0, execBuilder.exec(sshNode).getExitCode());
            assertEquals("alpine\n", sshNode.exec("cat", rErr).getOutput());

            // Piping commands
            ExecResult result = ExecBuilder.fromShellScript("echo $AHOJ $BYE | sed -e s#Hi#Hello# | tee /tmp/tee.out")
                    .environmentVariables(ImmutableMap.of("AHOJ", "Hi World!", "BYE", "Nashledanou."))
                    .exec(sshNode);
            assertEquals(0, result.getExitCode());
            assertThat(result.getOutput(), containsString("Hello World! Nashledanou."));
            assertEquals("Hello World! Nashledanou.\n", sshNode.exec("cat", "/tmp/tee.out").getOutput());

            // semicolon as a command separator
            result = ExecBuilder.fromShellScript("echo Ahoj; exit 2").exec(sshNode);
            assertEquals(2, result.getExitCode());
            assertThat(result.getOutput(), allOf(containsString("Ahoj"), not(containsString("exit"))));

            // new-line as a command separator
            result = ExecBuilder.fromShellScript("echo Ahoj\nexit 3").exec(sshNode);
            assertEquals(3, result.getExitCode());
            assertThat(result.getOutput(), allOf(containsString("Ahoj"), not(containsString("exit"))));

            // a more real-life example with script stored in a resource file
            result = ExecBuilder.fromShellScript(IOUtils.toString(getClass().getResourceAsStream("shell-script"), StandardCharsets.UTF_8)).exec(sshNode);
            assertEquals(0, result.getExitCode());
            assertThat(result.getOutput(), endsWith("Found 37 dirs\n"));
        }
    }

    @Test
    public void testEnvironmentVariables() throws OperationNotSupportedException, IOException, InterruptedException {
        try (Node sshNode = dockerProvider.createNode(NODENAME)) {
            // prepare a command which depends on variables
            ExecBuilder.fromShellScript("echo -e 'echo -n \"$@\"\\nexit $A' > /tmp/script; chmod +x /tmp/script").exec(sshNode)
                    .assertSuccess();

            ExecBuilder.fromCommand("/tmp/script", "x").environmentVariable("A", "1").exec(sshNode).assertFailure();
            ExecBuilder.fromCommand("/tmp/script", "y").environmentVariable("A", "0").exec(sshNode).assertSuccess();

            ExecResult execResult = ExecBuilder.fromCommand("/tmp/script", "whatever", "params")
                    .environmentVariables(ImmutableMap.of("A", "1", "B", "0")).environmentVariable("B", "42").exec(sshNode);
            execResult.assertFailure();
            assertThat(execResult.getOutput(), endsWith("whatever params"));

            execResult = ExecBuilder.fromCommand("/tmp/script", "whatever", "params")
                    .environmentVariables(ImmutableMap.of("A", "0", "B", "1")).exec(sshNode);
            execResult.assertSuccess();
            assertThat(execResult.getOutput(), endsWith("whatever params"));

            execResult = ExecBuilder.fromCommand("/tmp/script", "whatever", "params").environmentVariables(null).exec(sshNode);
            execResult.assertSuccess();
            assertThat(execResult.getOutput(), endsWith("whatever params"));

        }
    }

}
