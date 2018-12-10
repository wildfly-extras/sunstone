package org.wildfly.extras.sunstone.api.ssh;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.impl.DefaultExecResult;

/**
 * An SSH client that allows executing remote commands and copying files (in both directions).
 * Each instance of this class has a dedicated SSH connection, so reusing {@code SshClient}s
 * is a good idea. Users are responsible for closing the {@code SshClient}.
 */
public interface SshClient extends AutoCloseable {
    /**
     * Starts the {@code command} on the remote node and returns an object that allows interacting
     * with the remote process. Callers are responsible for closing the {@code CommandExecution}.
     *
     * @see CommandExecution
     */
    CommandExecution exec(String command);

    /**
     * Starts the {@code command} on the remote node and waits until it finishes running. Then returns an object
     * that contains useful information: stdout and stderr content as a {@code String} (interpreted as UTF-8)
     * and the exit code.
     */
    default ExecResult execAndWait(String command) throws IOException, InterruptedException {
        try (CommandExecution execution = exec(command)) {

            execution.await();

            try (InputStream stdout = execution.stdout(); InputStream stderr = execution.stderr()) {
                return new DefaultExecResult(
                        CharStreams.toString(new InputStreamReader(stdout, StandardCharsets.UTF_8)),
                        CharStreams.toString(new InputStreamReader(stderr, StandardCharsets.UTF_8)),
                        execution.exitCode().orElse(-1)
                );
            }
        }
    }

    /**
     * Starts the {@code command} on the remote node and waits for its completion for the specified amount of time.
     *
     * @param command
     * @param timeout how long to wait
     * @param timeoutUnit the {@link TimeUnit} of {@code timeout}
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    ExecResult execAndWait(String command, long timeout, TimeUnit timeoutUnit) throws IOException, InterruptedException, TimeoutException;

    /**
     * Downloads the content of remote file at {@code path}. Caller is responsible
     * for closing the returned {@code InputStream}.
     */
    InputStream get(String path) throws IOException;

    /**
     * Uploads the content of {@code data} to a remote file at {@code path}. The {@code data} supplier
     * will be consumed exactly once.
     */
    void put(String path, Supplier<InputStream> data);

    void close(); // redeclared without checked exceptions
}
