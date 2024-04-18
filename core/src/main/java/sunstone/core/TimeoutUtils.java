package sunstone.core;


public class TimeoutUtils {
    private static double factor = SunstoneConfigResolver.getValue(CoreConfig.TIMEOUT_FACTOR, 1.0);

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
