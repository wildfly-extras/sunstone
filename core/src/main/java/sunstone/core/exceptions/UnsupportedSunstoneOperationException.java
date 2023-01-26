package sunstone.core.exceptions;


/**
 * Exception show that some unsupported operation has been executed within Sunstone framework.
 */
public class UnsupportedSunstoneOperationException extends SunstoneException{
    private static final long serialVersionUID = 23252322112L;

    /**
     * Constructs the exception with custom message and cause provided.
     *
     * @param message custom exception message
     * @param cause the cause
     */
    public UnsupportedSunstoneOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedSunstoneOperationException(String message) {
        super(message);
    }

    public UnsupportedSunstoneOperationException(Throwable cause) {
        super(cause);
    }
}
