package sunstone.core;


public class CloudsConfig {

    public static final String TIMEOUT_FACTOR = "timeout.factor";

    public static final class WildFly {
        public static final String MNGMT_PORT = "wildfly.mngmt.port";
        public static final String MNGMT_USERNAME = "wildfly.mngmt.user";
        public static final String MNGMT_PASSWORD = "wildfly.mngmt.password";
        public static final String MNGMT_CONNECTION_TIMEOUT = "wildfly.mngmt.connection.timeout";
    }
}
