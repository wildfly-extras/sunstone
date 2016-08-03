package org.wildfly.extras.sunstone.api.wildfly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SunstoneWildFlyLogger {
    private SunstoneWildFlyLogger() {} // avoid instantiation

    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.wildfly");
}
