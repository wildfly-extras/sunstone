package org.wildfly.extras.sunstone.api.jclouds;

import org.jclouds.compute.ComputeServiceContext;
import org.wildfly.extras.sunstone.api.CloudProvider;

/**
 * Interface extending {@link CloudProvider} which gives access to its JClouds representation.
 */
public interface JCloudsCloudProvider extends CloudProvider {
    @Deprecated
    default ComputeServiceContext getJCloudsImpl() {
        return getComputeServiceContext();
    }

    /**
     * Returns backing JClouds {@link ComputeServiceContext} instance for this cloud provider.
     */
    ComputeServiceContext getComputeServiceContext();
}
