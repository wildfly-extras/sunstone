package org.wildfly.extras.sunstone.api.impl;

import com.google.common.base.Strings;

public final class Constants {
    private Constants() {} // avoid instantiation

    /** The {@code sst-} prefix. */
    public static final String SUNSTONE_PREFIX = "sst-";

    /**
     * Default node group name which will be used for creating JClouds NodeMetadata.
     * Generated from current environment. That is: if running as a part of Jenkins job, it will contain the current
     * build tag, otherwise it will contain the current username.
     */
    public static final String JCLOUDS_NODEGROUP;

    static {
        // https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables
        String buildTag = System.getenv("BUILD_TAG");
        String userName = System.getProperty("user.name");

        String prefixPart = Strings.isNullOrEmpty(buildTag) ? userName : buildTag;
        JCLOUDS_NODEGROUP = SUNSTONE_PREFIX + prefixPart;
    }
}
