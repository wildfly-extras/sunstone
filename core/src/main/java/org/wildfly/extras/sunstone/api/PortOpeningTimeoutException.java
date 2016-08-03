package org.wildfly.extras.sunstone.api;

/**
 * Exception to be thrown when a port fails to open in a requested time limit.
 *
 */
public class PortOpeningTimeoutException extends PortOpeningException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs the exception with custom message provided.
     *
     * @param portNumber port number which failed to open in a requested time
     * @param message custom exception message
     */
    public PortOpeningTimeoutException(int portNumber, String message) {
        super(portNumber, message);
    }

    /**
     * Constructs the exception with default message.
     *
     * @param portNumber port number which failed to open in a requested time
     */
    public PortOpeningTimeoutException(int portNumber) {
        super(portNumber, "Port " + portNumber + " has not opened in requested time.");
    }

}
