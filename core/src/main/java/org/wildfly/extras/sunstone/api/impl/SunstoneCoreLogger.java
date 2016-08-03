package org.wildfly.extras.sunstone.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SunstoneCoreLogger {
    private SunstoneCoreLogger() {} // avoid instantiation

    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.core");

    public static final Logger SSH = LoggerFactory.getLogger("sunstone.core.ssh");
}
