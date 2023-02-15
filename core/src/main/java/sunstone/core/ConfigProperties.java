package sunstone.core;


public class ConfigProperties {

    public static final String TIMEOUT_FACTOR = "sunstone.timeout.factor";

    public static final class WildFly {
        public static final String MNGMT_PORT = "sunstone.wildfly.mngmt.port";
        public static final String MNGMT_USERNAME = "sunstone.wildfly.mngmt.user";
        public static final String MNGMT_PASSWORD = "sunstone.wildfly.mngmt.password";
        public static final String MNGMT_CONNECTION_TIMEOUT = "sunstone.wildfly.mngmt.connection.timeout";
        public static final String MNGMT_HOST = "sunstone.wildfly.mngmt.host";
        public static final String MNGMT_PROFILE = "sunstone.wildfly.mngmt.profile";
    }
}
