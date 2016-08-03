package org.wildfly.extras.sunstone.api;

import java.io.IOException;
import java.nio.file.Path;

import org.wildfly.extras.sunstone.api.ssh.SshClient;

/**
 * Single instance (virtual machine) in a cloud. It's created from a {@link CloudProvider}.
 */
public interface Node extends AutoCloseable {
    /**
     * Returns node name.
     */
    String getName();

    /**
     * Returns image name used. This is implementation-dependent and doesn't have to be directly usable for interacting
     * with the cloud provider. For example, if a cloud provider uses a notion of a human-readable but ambiguous
     * image name and also a unique but unreadable image ID, the return value can combine both values in an arbitrary
     * fashion.
     */
    String getImageName();

    /**
     * Returns the provider instance which created this node.
     */
    CloudProvider getCloudProvider();

    /**
     * Returns address on which the node is reachable.
     */
    String getPublicAddress();

    /**
     * Returns address on which the node is reachable in the cloud internally (from other nodes in the same cloud).
     */
    String getPrivateAddress();

    /**
     * Returns the publicly-available port number that corresponds to the node-internal port number {@code tcpPort},
     * if it exists. In other words, performs port mapping.
     *
     * <ul>
     *     <li>
     *         If all the ports on this node are exposed to the public world directly (typical e.g. with EC2 or Azure),
     *         there is no port mapping and given port number is returned unchanged.
     *     </li>
     *     <li>
     *         If the number of ports exposed to the public world is limited (typical e.g. with Docker), the publicly
     *         available port number is often different from the node-internal port number and port mapping is needed.
     *         It works like this:
     *         <ul>
     *             <li>
     *                 If a given node-internal port {@code tcpPort} is exposed to the public world, this method returns
     *                 a port number that is available on {@link #getPublicAddress()} and is mapped to the internal
     *                 port.
     *             </li>
     *             <li>
     *                 If a given node-internal port is <i>not</i> exposed to the public world, this method returns
     *                 {@code -1}.
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     */
    int getPublicTcpPort(int tcpPort);

    /**
     * Checks if the given port is open/listening on this node. The {@code portNr} argument is not mapped.
     * Implementation is responsible for checking correct public port. Return {@code true} if the port has
     * its representation on public interface and the public port is open.
     *
     * @param portNr port number (internal) - if port mapping is used, then implementation must convert the value
     *               to public port and check the public value.
     * @return if given port is open on this node and is available on public address
     */
    boolean isPortOpen(int portNr);

    /**
     * Waits until given ports are open or the given timeout is reached. Unless specified in implementing documentation,
     * the worst case scenario is that the method will wait for {@code timeoutSeconds * portNrs.length} seconds.
     * If the implementing Node uses port mapping, then the provided port numbers are the internal ones and
     * the Node is responsible for mapping them to their public counterparts.
     *
     * @param timeoutSeconds maximum wait time in seconds
     * @param portNrs port numbers (unmapped) to be wait for
     * @throws PortOpeningException when a port doesn't open for some reason (timeout or configuration problem)
     */
    void waitForPorts(long timeoutSeconds, int... portNrs) throws PortOpeningException;

    /**
     * Checks if this node is running and reachable. This is interpreted in a strict sense: the operating system
     * is "up", ready to accept commands, and there's no sign of it going away. For example, if the node is implemented
     * as a virtual machine, it isn't booting, it isn't suspending, etc. Note that in general, the return value
     * of this method can immediately be out of date.
     */
    boolean isRunning() throws OperationNotSupportedException;

    /**
     * Executes given command on this node. Note that even if {@code command} is an array of {@code String}s,
     * it is still <b>one</b> command, split into individual parts. For example, to execute {@code ls -l},
     * you should call {@code node.exec("ls", "-l")}.
     */
    ExecResult exec(String... command) throws OperationNotSupportedException, IOException, InterruptedException;

    /**
     * Stops this node gracefully (i.e. clean shutdown).
     *
     * @throws OperationNotSupportedException
     */
    void stop() throws OperationNotSupportedException;

    /**
     * Start again this node (after stop or kill).
     *
     * @throws OperationNotSupportedException
     */
    void start() throws OperationNotSupportedException;

    /**
     * Forces immediate stop of this node. Not all cloud providers have to offer such functionality.
     * If the cloud provider doesn't support a distinct "kill" operation, this method is equivalent to {@link #stop()}.
     *
     * @throws OperationNotSupportedException
     */
    void kill() throws OperationNotSupportedException;

    /**
     * Copies file/folder on given location in this node to given local {@link Path}.
     */
    void copyFileFromNode(String remoteSrc, Path localTarget) throws OperationNotSupportedException, IOException, InterruptedException;

    /**
     * Copies file/folder on local {@link Path} to given target location in this node.
     */
    void copyFileToNode(Path localSrc, String remoteTarget) throws OperationNotSupportedException, IOException, InterruptedException;

    /**
     * <p>Provides an SSH connection for advanced usecases. Use {@link #exec(String...)},
     * {@link org.wildfly.extras.sunstone.api.process.ExecBuilder ExecBuilder}, {@link #copyFileFromNode(String, Path)} and
     * {@link #copyFileToNode(Path, String)} to satisfy your basic needs. These methods can also be more optimized
     * in case SSH is not necessary, e.g. on Docker.</p>
     *
     * <p>It is the caller's responsibility to {@code close} the returned {@link SshClient}.</p>
     *
     * @throws OperationNotSupportedException when connecting to SSH fails, presumably because there's no SSH server
     * @throws InterruptedException when interrupted while waiting for SSH to connect
     */
    SshClient ssh() throws OperationNotSupportedException, InterruptedException;

    /**
     * Returns the node configuration entry with given name. If such entry is not found, then given default value
     * is returned.
     *
     * @deprecated use {@link #config()} instead
     */
    @Deprecated
    String getProperty(String propertyName, String defaultValue);

    /** Returns the {@link ConfigProperties configuration properties} of this node. */
    ConfigProperties config();

    /**
     * Destroys this node (and returns resources to cloud provider).
     */
    void close();
}
