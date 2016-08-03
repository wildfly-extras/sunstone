package org.wildfly.extras.sunstone.arquillian;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SunstoneArquillianLogger {
    private SunstoneArquillianLogger() {} // avoid instantiation

    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.arquillian");
}
