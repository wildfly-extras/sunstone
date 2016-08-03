package org.wildfly.extras.sunstone.api.impl;

import org.wildfly.extras.sunstone.api.ExecResult;

/**
 * Default implementation of {@link ExecResult}. Just a DTO.
 *
 */
public final class DefaultExecResult implements ExecResult {

    private final String output;
    private final String error;
    private final int exitCode;

    public DefaultExecResult(String output, String error, int exitCode) {
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.wildfly.extras.sunstone.api.ExecResult#getOutput()
     */
    public String getOutput() {
        return output;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.wildfly.extras.sunstone.api.ExecResult#getError()
     */
    public String getError() {
        return error;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.wildfly.extras.sunstone.api.ExecResult#getExitCode()
     */
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public String toString() {
        return "DefaultExecResult [output=" + output + ", error=" + error + ", exitCode=" + exitCode + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + exitCode;
        result = prime * result + ((output == null) ? 0 : output.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultExecResult other = (DefaultExecResult) obj;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (exitCode != other.exitCode)
            return false;
        if (output == null) {
            if (other.output != null)
                return false;
        } else if (!output.equals(other.output))
            return false;
        return true;
    }

}
