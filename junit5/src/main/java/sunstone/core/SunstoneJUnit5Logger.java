package sunstone.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SunstoneJUnit5Logger {
    private SunstoneJUnit5Logger() {} // avoid instantiation
    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.junit5");

}
