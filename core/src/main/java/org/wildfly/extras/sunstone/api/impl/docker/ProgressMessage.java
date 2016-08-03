package org.wildfly.extras.sunstone.api.impl.docker;

/**
 * JSON mapping object for progress messages returned in inputstream a docker ImageApi.createImage() method call.
 *
 */
public class ProgressMessage {

    private String status;
    private String progress;
    private String id;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (id != null) {
            sb.append(id).append(": ");
        }
        sb.append(status);
        if (progress != null) {
            sb.append(": ").append(progress);
        }

        return sb.toString();
    }
}
