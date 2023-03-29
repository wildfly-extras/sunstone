package sunstone.azure.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzureWFLogger {
    private AzureWFLogger() {} // avoid instantiation
    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.azure");

}
