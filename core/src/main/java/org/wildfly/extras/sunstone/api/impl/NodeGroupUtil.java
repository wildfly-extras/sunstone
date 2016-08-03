package org.wildfly.extras.sunstone.api.impl;

import com.google.common.base.Strings;

/**
 * Utilities for working with a node group. That is a JClouds-specific concept.
 */
public final class NodeGroupUtil {
    private NodeGroupUtil() {} // avoid instantiation

    /**
     * Figures out a correct group name for a node in a cloud provider.
     * Falls back to {@link Constants#JCLOUDS_NODEGROUP}, which is generated from current environment.
     */
    public static String nodeGroupName(ObjectProperties nodeProperties, ObjectProperties cloudProviderProperties) {
        String nodeGroup = nodeProperties.getProperty(Config.GROUP);
        if (Strings.isNullOrEmpty(nodeGroup)) {
            nodeGroup = cloudProviderProperties.getProperty(Config.GROUP);
        }
        if (Strings.isNullOrEmpty(nodeGroup)) {
            nodeGroup = Constants.JCLOUDS_NODEGROUP;
        }
        return nodeGroup;
    }
}
