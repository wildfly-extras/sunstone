package org.wildfly.extras.sunstone.api;

/**
 * Exception to be thrown when resource loading fails for any reason.
 */
public class ResourceLoadingException extends RuntimeException {
    private static final long serialVersionUID = 2L;

    /**
     * Constructs the exception with custom message and cause provided.
     *
     * @param message custom exception message
     * @param cause the cause
     */
    public ResourceLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceLoadingException(String message) {
        super(message);
    }

    public ResourceLoadingException(Throwable cause) {
        super(cause);
    }
}