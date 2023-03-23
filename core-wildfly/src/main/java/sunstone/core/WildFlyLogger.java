package sunstone.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WildFlyLogger {
    private WildFlyLogger() {} // avoid instantiation
    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.core");

}
