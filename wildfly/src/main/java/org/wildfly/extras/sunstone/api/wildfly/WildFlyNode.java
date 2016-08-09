package org.wildfly.extras.sunstone.api.wildfly;

import java.io.IOException;

import org.slf4j.Logger;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.NodeWrapper;

/**
 * Adds WildFly functionality to Node instances. The WildFly configuration is based on objectProperties of the node. Check
 * constants in {@link WildFlyNodeConfig} class to get possible config entries. This is only a wrapper class which adds few JBoss AS
 * related methods and the calls to other methods are delegated to the wrapped {@link Node}.
 */
public class WildFlyNode extends NodeWrapper {
    private static final Logger LOGGER = SunstoneWildFlyLogger.DEFAULT;

    private final int mgmtPortUnmapped;
    private final String mgmtUser;
    private final String mgmtPassword;

    private final boolean isDomain;
    private final String defaultProfile;
    private final String defaultHost;

    /**
     * Constructor to wrap an existing node.
     *
     * @param node not-<code>null</code> Node instance
     */
    public WildFlyNode(Node node) {
        super(node);
        final String mgmtPortStr = node.config().getProperty(WildFlyNodeConfig.MGMT_PORT, null);
        if (mgmtPortStr != null) {
            mgmtPortUnmapped = Integer.valueOf(mgmtPortStr);
        } else {
            // defaulting to well known mgmt ports
            mgmtPortUnmapped = node.isPortOpen(9990) ? 9990 : 9999;
        }
        mgmtUser = node.config().getProperty(WildFlyNodeConfig.MGMT_USER, null);
        mgmtPassword = node.config().getProperty(WildFlyNodeConfig.MGMT_PASSWORD, null);

        String operatingMode = node.config().getProperty(WildFlyNodeConfig.MGMT_MODE, "standalone");
        isDomain = "domain".equals(operatingMode);
        defaultProfile = node.config().getProperty(WildFlyNodeConfig.MGMT_DEFAULT_PROFILE, null);
        defaultHost = node.config().getProperty(WildFlyNodeConfig.MGMT_DEFAULT_HOST, null);
    }

    /**
     * Returns port number of the management port. The returned value is the value mapped to the public port
     * (if the wrapped node implements port mappings).
     */
    public int getMgmtPort() {
        return getPublicTcpPort(mgmtPortUnmapped);
    }

    /**
     * Returns management user configured.
     */
    public String getMgmtUser() {
        return mgmtUser;
    }

    /**
     * Returns password for the management user.
     */
    public String getMgmtPassword() {
        return mgmtPassword;
    }

    /**
     * Returns timeout (in seconds) used for {@link #createManagementClient()} method.
     */
    public long getMgmtConnectionTimeoutInSec() {
        return config().getPropertyAsLong(WildFlyNodeConfig.MGMT_CONNECTION_TIMEOUT_IN_SEC, 60);
    }

    /**
     * Returns timeout (in seconds) used for waiting for management port to open in {@link #waitUntilRunning()} mehod.
     */
    public long getMgmtPortOpeningTimeoutInSec() {
        return config().getPropertyAsLong(WildFlyNodeConfig.MGMT_PORT_OPENING_TIMEOUT_IN_SEC, 60);
    }

    /**
     * Returns timeout (in seconds) used for waiting for the server to finish booting after its management interface becomes available.
     */
    public long getBootTimeoutInSec() {
        return config().getPropertyAsLong(WildFlyNodeConfig.MGMT_BOOT_TIMEOUT_IN_SEC, 60);
    }

    /**
     * Creates Creaper {@link OnlineManagementClient}, which can be used for server configuration.
     * The client connection timeout configuration comes from Node property {@value WildFlyNodeConfig#MGMT_CONNECTION_TIMEOUT_IN_SEC}.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     */
    public OnlineManagementClient createManagementClient() throws IOException {
        return createManagementClient(1000 * (int) getMgmtConnectionTimeoutInSec());
    }

    /**
     * As {@link WildFlyNode#createManagementClient()}, but you can define a positive connection
     * timeout in milliseconds.
     */
    public OnlineManagementClient createManagementClient(int timeoutInMillis) throws IOException {
        OnlineOptions.ConnectionOnlineOptions options = isDomain
                ? OnlineOptions.domain().forProfile(defaultProfile).forHost(defaultHost).build()
                : OnlineOptions.standalone();

        OnlineOptions.OptionalOnlineOptions clientOptions = options
                .hostAndPort(getPublicAddress(), getMgmtPort())
                .auth(mgmtUser, mgmtPassword)
                .connectionTimeout(timeoutInMillis)
                .bootTimeout(1000 * (int) getBootTimeoutInSec());

        return ManagementClient.online(clientOptions.build());
    }

    /**
     * Waits until WildFly is in running state. If the given timeout value is greater than 0, then it'll also wait (at most the
     * given amount of seconds) until the management port is open on the wrapped node.
     */
    public void waitUntilRunning(long timeoutForPortInSeconds) throws IOException {
        if (timeoutForPortInSeconds > 0) {
            LOGGER.debug("Waiting for management port on node '{}'", getName());
            waitForPorts(timeoutForPortInSeconds, mgmtPortUnmapped);
            LOGGER.debug("Management port {} is open on node '{}'", mgmtPortUnmapped, getName());
        }
        try (OnlineManagementClient managementClient = createManagementClient()) {
            Administration admin = new Administration(managementClient);
            LOGGER.debug("Waiting for WildFly server state running on node '{}'", getName());
            admin.waitUntilRunning();
            LOGGER.debug("WildFly Server is running on node '{}'", getName());
        }
    }


    /**
     * Waits until WildFly is in running state.
     */
    public void waitUntilRunning() throws IOException {
        waitUntilRunning(getMgmtPortOpeningTimeoutInSec());
    }
}
