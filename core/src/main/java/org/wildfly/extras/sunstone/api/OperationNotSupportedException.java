package org.wildfly.extras.sunstone.api;

/**
 * Exception to be thrown when a requested operation is not supported by a cloud provider or node.
 *
 */
public class OperationNotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationNotSupportedException() {
        super();
    }

    public OperationNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationNotSupportedException(String message) {
        super(message);
    }

    public OperationNotSupportedException(Throwable cause) {
        super(cause);
    }

}
