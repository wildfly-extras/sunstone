package org.wildfly.extras.sunstone.api.jclouds;

import org.jclouds.compute.domain.NodeMetadata;
import org.wildfly.extras.sunstone.api.Node;

/**
 * Interface extending {@link Node} which gives access to its JClouds representation.
 */
public interface JCloudsNode extends Node {

    /**
     * Returns the JClouds {@link NodeMetadata} that reflect the node state as of the time of calling this method.
     * Performs network communication (potentially multiple request/response cycles), so it's fairly slow.
     */
    NodeMetadata getFreshNodeMetadata();

    /**
     * Returns the JClouds {@link NodeMetadata} that reflect the node state as of the time the node was created.
     * Doesn't perform any network communication, so it's way faster than {@link #getFreshNodeMetadata()}, but it's
     * only usable when stale data aren't a problem. E.g. when you only need the node ID.
     */
    NodeMetadata getInitialNodeMetadata();
}
