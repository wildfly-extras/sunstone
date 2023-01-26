package azure.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzureLogger {
    private AzureLogger() {} // avoid instantiation
    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.clouds.azure");

}
