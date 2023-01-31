package sunstone.aws.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AwsLogger {
    private AwsLogger() {} // avoid instantiation
    public static final Logger DEFAULT = LoggerFactory.getLogger("sunstone.aws");

}
