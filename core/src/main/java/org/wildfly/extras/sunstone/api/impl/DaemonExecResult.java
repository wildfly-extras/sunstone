package org.wildfly.extras.sunstone.api.impl;

import org.wildfly.extras.sunstone.api.ExecResult;

public final class DaemonExecResult implements ExecResult {
    public static final ExecResult INSTANCE = new DaemonExecResult();

    private DaemonExecResult() {
    }

    @Override
    public String getOutput() {
        return "";
    }

    @Override
    public String getError() {
        return "";
    }

    @Override
    public int getExitCode() {
        return 0;
    }

    // ---

    @Override
    public void assertSuccess(String message) {
        throw new IllegalStateException("There's no exit code, process was executed as daemon");
    }

    @Override
    public void assertFailure(String message) {
        throw new IllegalStateException("There's no exit code, process was executed as daemon");
    }

    @Override
    public void assertOutputContains(String text, String message) {
        throw new IllegalStateException("There's no stdout/stderr, process was executed as daemon");
    }

    @Override
    public void assertOutputDoesntContain(String text, String message) {
        throw new IllegalStateException("There's no stdout/stderr, process was executed as daemon");
    }

    @Override
    public String toString() {
        return "DaemonExecResult [getOutput()=" + getOutput() + ", getError()=" + getError() + ", getExitCode()="
                + getExitCode() + "]";
    }
}
