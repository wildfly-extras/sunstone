package org.wildfly.extras.sunstone.arquillian;

/**
 * Config entries (object property names) for Arquillian types.
 */
public final class ArquillianConfig {

    public static final String SYSTEM_PROPERTY_ARQUILLIAN_SUITE = "sunstone.arquillian.suite";
    public static final String SYSTEM_PROPERTY_DISABLE_EXTENSION = "sunstone.arquillian.disable";

    public static final class Node {
        public static final String PROVIDER = "arquillian.provider";
        public static final String CONTAINER_REGISTER = "arquillian.container.register";
        public static final String CONTAINER_IS_DEFAULT = "arquillian.container.isDefault";
    }

    public static final class Suite {
        /**
         * Arquillian nodes to be created for suite.
         */
        public static final String START_NODES = "start.nodes";
        public static final String DESTROY_PROVIDERS = "destroy.providers";
    }

    private ArquillianConfig() {
        // don't instantiate
    }

}
