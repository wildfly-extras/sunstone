package sunstone.core;

import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;


public class TimeoutUtils {
    private static double factor = Double.parseDouble(new ObjectProperties(ObjectType.CLOUDS, null).getProperty(CloudsConfig.TIMEOUT_FACTOR, "1.0"));

     public static long adjust(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        } else {
            return Math.round(amount * factor);
        }
    }

    public static double getFactor() {
        return factor;
    }
}
