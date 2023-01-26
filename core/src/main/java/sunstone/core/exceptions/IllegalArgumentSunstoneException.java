package sunstone.core.exceptions;


/**
 * The exceptions show an illegal argument / inout has been used in context of Sunstone.
 */
public class IllegalArgumentSunstoneException extends SunstoneException{
    private static final long serialVersionUID = 23252322112L;

    /**
     * Constructs the exception with custom message and cause provided.
     *
     * @param message custom exception message
     * @param cause the cause
     */
    public IllegalArgumentSunstoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentSunstoneException(String message) {
        super(message);
    }

    public IllegalArgumentSunstoneException(Throwable cause) {
        super(cause);
    }
}
