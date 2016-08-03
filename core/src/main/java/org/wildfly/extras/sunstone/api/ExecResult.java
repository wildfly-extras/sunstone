package org.wildfly.extras.sunstone.api;

import org.wildfly.extras.sunstone.api.impl.AssertionUtil;

import com.google.common.base.Strings;

/**
 * Result of a command execution.
 */
public interface ExecResult {
    /**
     * Content read from standard output.
     */
    String getOutput();

    /**
     * Content read from error output.
     */
    String getError();

    /**
     * Exit code.
     */
    int getExitCode();

    // ---
    // assertions

    /**
     * Asserts that the {@link #getExitCode() exit code} is equal to zero.
     *
     * @throws AssertionError if the exit code is {@code != 0}
     */
    default void assertSuccess() {
        assertSuccess(null);
    }

    /**
     * Asserts that the {@link #getExitCode() exit code} is equal to zero.
     *
     * @param message will be used as a part of the assertion message
     * @throws AssertionError if the exit code is {@code != 0}
     */
    default void assertSuccess(String message) {
        int exitCode = getExitCode();
        if (exitCode != 0) {
            AssertionUtil.fail(message, "Expected success, but process finished with exit code " + exitCode);
        }
    }

    /**
     * Asserts that the {@link #getExitCode() exit code} is <i>not</i> equal to zero.
     *
     * @throws AssertionError if the exit code is {@code == 0}
     */
    default void assertFailure() {
        assertFailure(null);
    }

    /**
     * Asserts that the {@link #getExitCode() exit code} is <i>not</i> equal to zero.
     *
     * @param message will be used as a part of the assertion message
     * @throws AssertionError if the exit code is {@code == 0}
     */
    default void assertFailure(String message) {
        if (getExitCode() == 0) {
            AssertionUtil.fail(message, "Expected failure, but process finished with exit code 0");
        }
    }

    /**
     * Asserts that either the {@link #getOutput() standard output} or the {@link #getError() error output}
     * contains given {@code text}.
     *
     * @param text will be searched for in stdout/stderr
     * @throws AssertionError if neither stdout nor stderr contains given text
     */
    default void assertOutputContains(String text) {
        assertOutputContains(text, null);
    }

    /**
     * Asserts that either the {@link #getOutput() standard output} or the {@link #getError() error output}
     * contains given {@code text}.
     *
     * @param text will be searched for in stdout/stderr
     * @param message will be used as a part of the assertion message
     * @throws AssertionError if neither stdout nor stderr contains given text
     */
    default void assertOutputContains(String text, String message) {
        String stdout = Strings.nullToEmpty(getOutput());
        String stderr = Strings.nullToEmpty(getError());
        if (!stdout.contains(text) && !stderr.contains(text)) {
            AssertionUtil.fail(message, "Expected output to contains '" + text + "'");
        }
    }

    /**
     * Asserts that neither the {@link #getOutput() standard output} nor the {@link #getError() error output}
     * contains given {@code text}.
     *
     * @param text will be searched for in stdout/stderr
     * @throws AssertionError if either stdout or stderr contains given text
     */
    default void assertOutputDoesntContain(String text) {
        assertOutputDoesntContain(text, null);
    }

    /**
     * Asserts that neither the {@link #getOutput() standard output} nor the {@link #getError() error output}
     * contains given {@code text}.
     *
     * @param text will be searched for in stdout/stderr
     * @param message will be used as a part of the assertion message
     * @throws AssertionError if either stdout or stderr contains given text
     */
    default void assertOutputDoesntContain(String text, String message) {
        String stdout = Strings.nullToEmpty(getOutput());
        String stderr = Strings.nullToEmpty(getError());
        if (stdout.contains(text) || stderr.contains(text)) {
            AssertionUtil.fail(message, "Expected output to not contain '" + text + "'");
        }
    }
}
