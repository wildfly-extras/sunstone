package org.wildfly.extras.sunstone.api;

/**
 * Exception to be thrown when a port fails to open.
 *
 */
public class PortOpeningException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int portNumber;

    /**
     * Constructs the exception with custom message provided.
     *
     * @param portNumber port number which failed to open
     * @param message custom exception message
     */
    public PortOpeningException(int portNumber, String message) {
        super(message);
        this.portNumber = portNumber;
    }

    /**
     * Returns port number which failed to open in a requested time.
     */
    public int getPortNumber() {
        return portNumber;
    }

}
