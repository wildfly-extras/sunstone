package org.wildfly.extras.sunstone.api.wildfly;

/**
 * Configuration keys for {@link WildFlyNode} wrapper.
 */
public final class WildFlyNodeConfig {

    public static final String MGMT_PORT = "wildfly.management.port";
    public static final String MGMT_USER = "wildfly.management.user";
    public static final String MGMT_PASSWORD = "wildfly.management.password";
    public static final String MGMT_CONNECTION_TIMEOUT_IN_SEC = "wildfly.management.connectionTimeoutInSec";
    public static final String MGMT_PORT_OPENING_TIMEOUT_IN_SEC = "wildfly.management.portOpeningTimeoutInSec";

    public static final String MGMT_MODE = "wildfly.mode";
    public static final String MGMT_DEFAULT_PROFILE = "wildfly.domain.default.profile";
    public static final String MGMT_DEFAULT_HOST = "wildfly.domain.default.host";

    private WildFlyNodeConfig() {} // avoid instantiation

}
