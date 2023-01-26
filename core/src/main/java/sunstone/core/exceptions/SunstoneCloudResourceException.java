package sunstone.core.exceptions;


/**
 * Exception linked to deployed resources to various clouds.
 */
public class SunstoneCloudResourceException extends SunstoneException{
    private static final long serialVersionUID = 2124325253L;

    /**
     * Constructs the exception with custom message and cause provided.
     *
     * @param message custom exception message
     * @param cause the cause
     */
    public SunstoneCloudResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SunstoneCloudResourceException(String message) {
        super(message);
    }

    public SunstoneCloudResourceException(Throwable cause) {
        super(cause);
    }
}
