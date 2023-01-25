package sunstone.core;


import sunstone.core.properties.ObjectProperties;
import sunstone.core.properties.ObjectType;

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
