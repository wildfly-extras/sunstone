package org.wildfly.extras.sunstone.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.wildfly.extras.sunstone.api.ssh.SshClient;

/**
 * Provides a convenient implementation of the Node interface that can be subclassed by developers wishing to adapt behavior or
 * provide additional functionality. This class implements the Wrapper or Decorator pattern. Methods default to calling through
 * to the wrapped request object.
 *
 */
public class NodeWrapper implements Node {
    private final Node delegate;

    /**
     * Constructor to wrap an existing node.
     *
     * @param node not-<code>null</code> Node instance
     */
    public NodeWrapper(Node node) {
        delegate = Objects.requireNonNull(node, "Node to be wrapped has to be provided");
    }

    /**
     * Returns wrapped Node.
     */
    public final Node unwrap() {
        return delegate;
    }

    /**
     * Unwraps the provided Node (if needed) until a the instance found is not an instance of the NodeWrapper.
     *
     * @param node not-<code>null</code> Node instance
     * @return such a wrapped Node instance which is not {@code instanceof NodeWrapper}
     */
    public static final Node unwrapAll(Node node) {
        Objects.requireNonNull(node);
        while (node instanceof NodeWrapper) {
            node = ((NodeWrapper) node).unwrap();
        }
        return node;
    }

    // ----------------------------------------------------
    // add control to delegate for the rest of the methods

    public String getPublicAddress() {
        return delegate.getPublicAddress();
    }

    public boolean isPortOpen(int portNr) {
        return delegate.isPortOpen(portNr);
    }

    public boolean isRunning() throws OperationNotSupportedException {
        return delegate.isRunning();
    }

    public ExecResult exec(String... command) throws OperationNotSupportedException, IOException, InterruptedException {
        return delegate.exec(command);
    }

    public void stop() throws OperationNotSupportedException {
        delegate.stop();
    }

    public void start() throws OperationNotSupportedException {
        delegate.start();
    }

    public void kill() throws OperationNotSupportedException {
        delegate.kill();
    }

    public void close() {
        delegate.close();
    }

    public String getName() {
        return delegate.getName();
    }

    public CloudProvider getCloudProvider() {
        return delegate.getCloudProvider();
    }

    public void copyFileFromNode(String remoteSrc, Path localTarget) throws OperationNotSupportedException, IOException, InterruptedException {
        delegate.copyFileFromNode(remoteSrc, localTarget);
    }

    public void copyFileToNode(Path localSrc, String remoteTarget) throws OperationNotSupportedException, IOException, InterruptedException {
        delegate.copyFileToNode(localSrc, remoteTarget);
    }

    @Override
    public SshClient ssh() throws InterruptedException {
        return delegate.ssh();
    }

    public String getImageName() {
        return delegate.getImageName();
    }

    @Override
    public ConfigProperties config() {
        return delegate.config();
    }

    public int getPublicTcpPort(int tcpPort) {
        return delegate.getPublicTcpPort(tcpPort);
    }

    public void waitForPorts(long timeoutSeconds, int... portNrs) {
        delegate.waitForPorts(timeoutSeconds, portNrs);
    }

    public String getPrivateAddress() {
        return delegate.getPrivateAddress();
    }

}
