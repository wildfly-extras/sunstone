package org.wildfly.extras.sunstone.api.impl;

/**
 * Core ObjectProperties types.
 *
 */
public enum ObjectType implements ObjectPropertiesType {
    CLOUD_PROVIDER("cloud.provider", "cloud provider"),
    NODE("node", "node"),
    AWS_SDK("awsSDK", "AWS SDK"),
    AZURE_SDK("azureSDK", "Azure SDK");

    private final String propertyPrefix;
    private final String humanReadableName;

    ObjectType(String prefix, String humanReadableName) {
        this.propertyPrefix = prefix;
        this.humanReadableName = humanReadableName;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }
}
