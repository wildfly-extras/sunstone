package org.wildfly.extras.sunstone.api;

/**
 * Enum of all supported {@link CloudProvider} implementations.
 */
public enum CloudProviderType {
    DOCKER("docker", "Docker"),
    EC2("ec2", "Amazon EC2"),
    AZURE("azure", "Microsoft Azure"),
    AZURE_ARM("azure-arm", "Microsoft Azure (ARM)"),
    OPENSTACK("openstack", "OpenStack"),
    BARE_METAL("baremetal", "Bare Metal"),
    ;

    private final String label;
    private final String humanReadableName;

    CloudProviderType(String label, String humanReadableName) {
        this.label = label;
        this.humanReadableName = humanReadableName;
    }

    public String getLabel() {
        return label;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    public static CloudProviderType fromLabel(String label) throws IllegalArgumentException {
        for (CloudProviderType type : values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported provider type: " + label);
    }
}
