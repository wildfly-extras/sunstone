package org.wildfly.extras.sunstone.api.impl;

/**
 * Holder of generally useful configuration data of a node. Used for the JClouds-based implementation to pass
 * data between a cloud-provider-specific subclass and general {@link AbstractJCloudsNode}.
 */
public final class NodeConfigData {
    final String waitForPortsProperty;
    public final String waitForPortsTimeoutProperty;
    public final int waitForPortsDefaultTimeout;

    public NodeConfigData(String waitForPortsProperty, String waitForPortsTimeoutProperty, int defaultTimeout) {
        this.waitForPortsProperty = waitForPortsProperty;
        this.waitForPortsTimeoutProperty = waitForPortsTimeoutProperty;
        this.waitForPortsDefaultTimeout = defaultTimeout;
    }
}
