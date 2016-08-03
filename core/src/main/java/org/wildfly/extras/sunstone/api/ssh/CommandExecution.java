package org.wildfly.extras.sunstone.api.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A remote execution of a command via {@link SshClient}. Allows interacting with the process
 * while it is still running. Users are responsible for closing the {@code CommandExecution}.
 */
public interface CommandExecution extends AutoCloseable {
    OutputStream stdin();

    InputStream stdout();

    InputStream stderr();

    /**
     * Waits until the command execution finishes. When this method returns normally, {@link #exitCode()}
     * is guaranteed to return a non-empty value. Unlike {@link #await(long, TimeUnit)}, this method
     * waits indefinitely.
     *
     * @throws InterruptedException when an underlying {@code Thread.sleep} was interrupted
     */
    void await() throws InterruptedException;

    /**
     * Waits until the command execution finishes. When this method returns normally, {@link #exitCode()}
     * is guaranteed to return a non-empty value.
     *
     * @param timeout how long to wait
     * @param timeoutUnit the {@link TimeUnit} of {@code timeout}
     * @throws InterruptedException when an underlying {@code Thread.sleep} was interrupted
     * @throws TimeoutException when {@code timeout} passed by and the command execution still didn't finish
     */
    void await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException;

    /** Missing if the process is still running and the exit code is not available. */
    OptionalInt exitCode();

    /**
     * Destroys this {@code CommandExecution}. The stdin, stdout and stderr streams are closed, the execution
     * session is disconnected. This will typically lead to the process being destroyed, but if it's executed
     * under {@code nohup}, it's been detached already, so nothing will happen to it.
     */
    void close() throws IOException;
}
