package org.wildfly.extras.sunstone.api.process;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.ExecResult;
import org.wildfly.extras.sunstone.api.Node;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DaemonExecResult;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.ssh.SshClient;

/**
 * This class comes with advanced support for command execution - it adds possibility to run command on Nodes as a daemon (i.e.
 * {@code nohup}) or with {@code sudo}. The class is inspired by {@link ProcessBuilder}.
 * <p>
 * Current implementation depends on SSH available in Node, but this is implementation detail which can change in the future.
 * <p>
 * Sample usage:
 *
 * <code><pre>
 * ExecResult result = ExecBuilder
 *     .fromCommand("sed", "#s#umask 022#umask 002", "-i", "/etc/profile")
 *     .withSudo()
 *     .exec(sshNode);
 * if (result.getExitCode()==0) {
 *   // ...
 * }
 * </pre></code>
 *
 */
public class ExecBuilder {

    public enum RedirectMode {
        OVERWRITE, APPEND
    }

    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    private static final CharSequenceTranslator ESCAPE_ARG = new ArgumentEscaper();

    public static final ExecResult EXEC_RESULT_DAEMON = DaemonExecResult.INSTANCE;

    private final List<String> command;

    private boolean withSudo = false;
    private boolean asDaemon = false;
    private String redirectErr;
    private RedirectMode redirectModeErr;
    private String redirectOut;
    private RedirectMode redirectModeOut;
    private final Map<String, String> environmentVariables = new HashMap<String, String>();

    /**
     * Private constructor.
     *
     * @param command mandatory command (with optional arguments) to be executed
     * @throws IllegalArgumentException the provided command is empty or null
     */
    private ExecBuilder(String... command) throws IllegalArgumentException {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("Command for execution has to be provided.");
        }
        this.command = new ArrayList<>(command.length);
        for (String arg : command)
            this.command.add(arg);
    }

    /**
     * Creates instance from the given command. It's not possible to use variable expansion in the command parts
     * as the values are escaped and single-quoted.
     *
     * @param command mandatory command with optional arguments
     */
    public static ExecBuilder fromCommand(String... command) {
        return new ExecBuilder(command);
    }

    /**
     * Creates instance from the given shell script.
     *
     * @param script shell script to be executed (must be not-{@code null})
     */
    public static ExecBuilder fromShellScript(String script) {
        return new ExecBuilder("sh", "-c", Objects.requireNonNull(script, "Shell script has to be provided"));
    }

    /**
     * Executes the configured SSH command on given Node. SSH has to be available (and configured correctly) for the
     * {@code node}.
     *
     * @return the {@link #EXEC_RESULT_DAEMON} constant if the command is to be executed as a daemon
     * @throws OperationNotSupportedException SSH client could not be obtained or did not successfully connect
     * @throws IOException when network error occurs
     * @throws InterruptedException when interrupted while waiting for (non-daemon) command to finish
     */
    public ExecResult exec(Node node) throws OperationNotSupportedException, IOException, InterruptedException {
        Objects.requireNonNull(node, "Can't execute command on null Node");

        String renderedCommand = renderCommand(node);
        LOGGER.debug("Executing command on node '{}': {}", node.getName(), renderedCommand);

        try (SshClient ssh = node.ssh()) {
            ExecResult execResult = ssh.execAndWait(renderedCommand);
            if (asDaemon) {
                LOGGER.trace("Nohup execution result (will not be propagated): {}", execResult);
                execResult = EXEC_RESULT_DAEMON;
            }
            LOGGER.trace("ExecBuilder execution result: {}", execResult);
            return execResult;
        }
    }

    /**
     * Enables using {@code sudo} for this command.
     *
     * @return self
     */
    public ExecBuilder withSudo() {
        this.withSudo = true;
        return this;
    }

    /**
     * Enables running on background (as a daemon - i.e. {@code nohup})
     *
     * @return self
     */
    public ExecBuilder asDaemon() {
        this.asDaemon = true;
        return this;
    }

    /**
     * Sets redirecting error output of the given command into the given file path. If the {@code redirectErr} parameter is
     * {@code null} (default) or an empty string then the error output is either not redirected (for standard commands) or it is
     * redirected to {@code /dev/null} for background commands ({@link #asDaemon()} used).
     *
     * @param redirectErr file path (e.g.{@code "/tmp/command-err.log"}
     * @param redirectMode mode of redirect (if null is used then defaults to {@link RedirectMode#OVERWRITE})
     * @return self
     */
    public ExecBuilder redirectErr(String redirectErr, RedirectMode redirectMode) {
        this.redirectErr = redirectErr;
        this.redirectModeErr = redirectMode;
        return this;
    }

    /**
     * Sets redirecting error output of the given command into the given file path (rewriting file if it exists). If the
     * {@code redirectErr} parameter is {@code null} (default) or an empty string then the error output is either not redirected
     * (for standard commands) or it is redirected to {@code /dev/null} for background commands ({@link #asDaemon()} used).
     *
     * @param redirectErr file path (e.g.{@code "/tmp/command-err.log"}
     * @return self
     */
    public ExecBuilder redirectErr(String redirectErr) {
        return redirectErr(redirectErr, null);
    }

    /**
     * Sets redirecting standard output of the given command into the given file path. If the {@code redirectOut} parameter is
     * {@code null} (default) or an empty string then the standard output is either not redirected (for standard commands) or it
     * is redirected to {@code /dev/null} for background commands ({@link #asDaemon()} used).
     *
     * @param redirectOut file path (e.g.{@code "/tmp/command-out.log"}
     * @param redirectMode mode of redirect (if null is used then defaults to {@link RedirectMode#OVERWRITE})
     * @return self
     */
    public ExecBuilder redirectOut(String redirectOut, RedirectMode redirectMode) {
        this.redirectOut = redirectOut;
        this.redirectModeOut = redirectMode;
        return this;
    }

    /**
     * Sets redirecting standard output of the given command into the given file path (rewriting file if it exists). If the
     * {@code redirectOut} parameter is {@code null} (default) or an empty string then the standard output is either not
     * redirected (for standard commands) or it is redirected to {@code /dev/null} for background commands ({@link #asDaemon()}
     * used).
     *
     * @param redirectOut file path (e.g.{@code "/tmp/command-out.log"}
     * @return self
     */
    public ExecBuilder redirectOut(String redirectOut) {
        return redirectOut(redirectOut, null);
    }

    /**
     * Sets environment variables to be configured for the executed command. If the variables were set already, the new value
     * replaces the original one.
     *
     * @param environmentVariables Map of environment variables to be set ({@code export KEY='VALUE'}) before the script
     *        execution (may be {@code null})
     * @return self
     */
    public ExecBuilder environmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables.clear();
        if (environmentVariables != null) {
            this.environmentVariables.putAll(environmentVariables);
        }
        return this;
    }

    /**
     * Adds a single environment variable to the map. If the given variable was set already, the new value replaces the original
     * one. Compared to {@link #environmentVariables(Map)} method, this one doesn't remove existing entries.
     *
     * @param variableName environment variable name (not-{@code null})
     * @param variableValue environment variable value (not-{@code null})
     * @return self
     */
    public ExecBuilder environmentVariable(String variableName, String variableValue) {
        this.environmentVariables.put(
                Objects.requireNonNull(variableName, "Name of environment variable must be not null"),
                Objects.requireNonNull(variableValue, "Value of environment variable '" + variableName + "' must be not null"));
        return this;
    }

    @Override
    public String toString() {
        return "ExecBuilder [command=" + command + ", withSudo=" + withSudo + ", asDaemon=" + asDaemon + ", redirectErr="
                + redirectErr + ", redirectModeErr=" + redirectModeErr + ", redirectOut=" + redirectOut + ", redirectModeOut="
                + redirectModeOut + ", environmentVariables=" + environmentVariables + "]";
    }

    /**
     * Renders the SSH command which will be executed. Things which were taken to account are:
     * <ul>
     * <li>command arguments quoting</li>
     * <li>PTY allocation related issues (see also https://github.com/hierynomus/sshj/issues/194)</li>
     * <li>redirecting output to file under the root user when using sudo</li>
     * </ul>
     */
    private String renderCommand(final Node node) {
        StringBuilder escapedCommand = new StringBuilder();
        boolean hasEnv = false;

        String sudoCmd = "sudo -S";
        if (withSudo && node.getCloudProvider() instanceof AbstractJCloudsCloudProvider) {
            AbstractJCloudsCloudProvider cp = (AbstractJCloudsCloudProvider) node.getCloudProvider();
            final String sudoCmdProperty = cp.getProviderSpecificPropertyName(node.config(), Config.Node.Shared.SUDO_COMMAND);
            sudoCmd = node.config().getProperty(sudoCmdProperty, sudoCmd);
        }
        sudoCmd = sudoCmd + " ";

        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            escapedCommand.append("export ").append(entry.getKey()).append("=")
            .append(ESCAPE_ARG.translate(entry.getValue())).append("; ");
            hasEnv = true;
        }

        boolean addSpace = false;
        for (String arg : command) {
            if (addSpace) {
                escapedCommand.append(" ");
            } else {
                addSpace = true;
            }
            escapedCommand.append(ESCAPE_ARG.translate(arg));
        }

        StringBuilder exec = new StringBuilder();
        if (asDaemon) {
            final String stdOut = StringUtils.defaultIfEmpty(redirectOut, "/dev/null");
            final String stdErr = StringUtils.defaultIfEmpty(redirectErr, "/dev/null");
            escapedCommand.append(" 1").append(redirectStr(redirectModeOut)).append(ESCAPE_ARG.translate(stdOut));
            escapedCommand.append(" 2").append(redirectStr(redirectModeErr))
                    .append(stdOut.equals(stdErr) ? "&1" : ESCAPE_ARG.translate(stdErr));
            escapedCommand.append(" &");
            if (withSudo) {
                exec.append("nohup ").append(sudoCmd).append("sh -c ");
            } else {
                exec.append("nohup sh -c ");
            }
            exec.append(ESCAPE_ARG.translate(escapedCommand));
            exec.append(" 1>/dev/null 2>/dev/null </dev/null");
        } else {
            if (StringUtils.isNotEmpty(redirectOut)) {
                escapedCommand.append(" 1").append(redirectStr(redirectModeOut)).append(ESCAPE_ARG.translate(redirectOut));
            }
            if (StringUtils.isNotEmpty(redirectErr)) {
                escapedCommand.append(" 2").append(redirectStr(redirectModeErr));
                if (redirectErr.equals(redirectOut)) {
                    escapedCommand.append("&1");
                } else {
                    escapedCommand.append(ESCAPE_ARG.translate(redirectErr));
                }
            }
            if (withSudo) {
                exec.append(sudoCmd);
            }
            // sudo with subshell to cover redirects into a file with root privileges
            if (withSudo || hasEnv) {
                exec.append("sh -c ");
                exec.append(ESCAPE_ARG.translate(escapedCommand));
            } else {
                exec.append(escapedCommand);
            }

        }
        return exec.toString();
    }

    /**
     * Returns "&gt;&gt;" for {@link RedirectMode#APPEND} parameter, "&gt;" otherwirse.
     *
     * @param redirectMode
     */
    private static String redirectStr(final RedirectMode redirectMode) {
        return RedirectMode.APPEND == redirectMode ? ">>" : ">";
    }

    /**
     * Escaper for shell command arguments.
     */
    private static class ArgumentEscaper extends CharSequenceTranslator {

        private static final String QUOTE_STR = "'";
        private static final String ESCAPED_QUOTE_STR = "'\\''";

        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            if (index != 0) {
                throw new IllegalArgumentException("ArgumentEscaper works only for index=0");
            }

            out.write(QUOTE_STR);
            out.write(StringUtils.replace(input.toString(), QUOTE_STR, ESCAPED_QUOTE_STR));
            out.write(QUOTE_STR);
            return Character.codePointCount(input, 0, input.length());
        }
    }

}
