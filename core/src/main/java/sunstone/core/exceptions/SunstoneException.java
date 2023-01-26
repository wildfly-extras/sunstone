package sunstone.core.exceptions;


/**
 * General Sunstone exception.
 */
public class SunstoneException extends Exception {
    private static final long serialVersionUID = 3253253222L;

    /**
     * Constructs the exception with custom message and cause provided.
     *
     * @param message custom exception message
     * @param cause the cause
     */
    public SunstoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public SunstoneException(String message) {
        super(message);
    }

    public SunstoneException(Throwable cause) {
        super(cause);
    }
}
