package org.wildfly.extras.sunstone.tests;

import java.util.Map;

import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.Node;

public interface TestedCloudProvider {
    /** The {@link CloudProviderType type} of the tested cloud provider. */
    CloudProviderType type();

    boolean hasImages();

    boolean hasPortMapping();

    boolean commandExecutionSupported();

    boolean execBuilderSupported();

    boolean lifecycleControlSupported();

    boolean fileCopyingSupported();

    /**
     * A {@code Map} of overrides that will be passed to
     * {@link org.wildfly.extras.sunstone.api.CloudProvider#create(String, Map) CloudProvider.create}.
     * It must cause the cloud provider to be unable to create node. For example, it can contain properties that
     * set wrong authentication data. Can return {@code null} if it isn't possible to devise such a set of overrides.
     */
    Map<String, String> overridesThatPreventCreatingNode();


    /**
     * Method used to retrieve private (not-mapped) port number on which SSH server in given node was started. The default
     * implementation returns {@code 22}.
     *
     * @return unmapped SSH port number.
     */
    default int getPrivateSshPort(Node node) {
        return 22;
    }
}
