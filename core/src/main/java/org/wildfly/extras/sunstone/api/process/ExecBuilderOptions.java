package org.wildfly.extras.sunstone.api.process;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

/**
 * Configuration object for {@link ExecBuilder}
 */
public class ExecBuilderOptions {

    private boolean withSudo;
    private boolean asDaemon;
    private String redirectErr;
    private ExecBuilder.RedirectMode redirectModeErr;
    private String redirectOut;
    private ExecBuilder.RedirectMode redirectModeOut;
    private Map<String, String> environmentVariables;
    private CharSequenceTranslator argumentEscaper;

    private ExecBuilderOptions(Data data) {
        this.withSudo = data.withSudo;
        this.asDaemon = data.asDaemon;
        this.redirectErr = data.redirectErr;
        this.redirectModeErr = data.redirectModeErr;
        this.redirectOut = data.redirectOut;
        this.redirectModeOut = data.redirectModeOut;
        this.environmentVariables = data.environmentVariables;
        this.argumentEscaper = data.argumentEscaper;
    }

    boolean isWithSudo() {
        return withSudo;
    }

    boolean isAsDaemon() {
        return asDaemon;
    }

    String getRedirectErr() {
        return redirectErr;
    }

    ExecBuilder.RedirectMode getRedirectModeErr() {
        return redirectModeErr;
    }

    String getRedirectOut() {
        return redirectOut;
    }

    ExecBuilder.RedirectMode getRedirectModeOut() {
        return redirectModeOut;
    }

    Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    CharSequenceTranslator getArgumentEscaper() {
        return argumentEscaper;
    }

    /**
     * Creates default {@link ExecBuilder} configuration
     */
    public static ExecBuilderOptions defaultOptions() {
        return new Builder().build();
    }

    /**
     * Creates builder for {@link ExecBuilder} configuration
     */
    public static Builder custom() {
        return new Builder();
    }

    public static final class Builder {
        private Data data;

        private Builder() {
            data = new Data();
        }

        /**
         * Enables using {@code sudo} for this command.
         *
         * @return self
         */
        public Builder withSudo() {
            data.withSudo = true;
            return this;
        }

        /**
         * Enables running on background (as a daemon - i.e. {@code nohup})
         *
         * @return self
         */
        public Builder asDaemon() {
            data.asDaemon = true;
            return this;
        }

        /**
         * Sets redirecting error output of the given command into the given file path. If the {@code redirectErr} parameter is
         * {@code null} (default) or an empty string then the error output is either not redirected (for standard commands) or it is
         * redirected to {@code /dev/null} for background commands ({@link #asDaemon()} used).
         *
         * @param redirectErr file path (e.g.{@code "/tmp/command-err.log"}
         * @param redirectMode mode of redirect (if null is used then defaults to {@link ExecBuilder.RedirectMode#OVERWRITE})
         * @return self
         */
        public Builder redirectErr(String redirectErr, ExecBuilder.RedirectMode redirectMode) {
            data.redirectErr = redirectErr;
            data.redirectModeErr = redirectMode;
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
        public Builder redirectErr(String redirectErr) {
            return redirectErr(redirectErr, null);
        }

        /**
         * Sets redirecting standard output of the given command into the given file path. If the {@code redirectOut} parameter is
         * {@code null} (default) or an empty string then the standard output is either not redirected (for standard commands) or it
         * is redirected to {@code /dev/null} for background commands ({@link #asDaemon()} used).
         *
         * @param redirectOut file path (e.g.{@code "/tmp/command-out.log"}
         * @param redirectMode mode of redirect (if null is used then defaults to {@link ExecBuilder.RedirectMode#OVERWRITE})
         * @return self
         */
        public Builder redirectOut(String redirectOut, ExecBuilder.RedirectMode redirectMode) {
            data.redirectOut = redirectOut;
            data.redirectModeOut = redirectMode;
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
        public Builder redirectOut(String redirectOut) {
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
        public Builder environmentVariables(Map<String, String> environmentVariables) {
            data.environmentVariables.clear();
            if (environmentVariables != null) {
                data.environmentVariables.putAll(environmentVariables);
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
        public Builder environmentVariable(String variableName, String variableValue) {
            data.environmentVariables.put(
                    Objects.requireNonNull(variableName, "Name of environment variable must be not null"),
                    Objects.requireNonNull(variableValue, "Value of environment variable '" + variableName + "' must be not null"));
            return this;
        }

        /**
         * Escaper for shell command arguments.
         *
         * @param argumentEscaper
         * @return self
         */
        public Builder argumentEscaper(CharSequenceTranslator argumentEscaper) {
            Objects.requireNonNull(argumentEscaper);
            data.argumentEscaper = argumentEscaper;
            return this;
        }

        public ExecBuilderOptions build() {
            return new ExecBuilderOptions(data);
        }

        @Override
        public String toString() {
            return "ExecBuilder [withSudo=" + data.withSudo + ", asDaemon=" + data.asDaemon + ", redirectErr="
                    + data.redirectErr + ", redirectModeErr=" + data.redirectModeErr + ", redirectOut=" + data.redirectOut + ", redirectModeOut="
                    + data.redirectModeOut + ", environmentVariables=" + data.environmentVariables + ", argumentEscaper=" + data.argumentEscaper + "]";
        }
    }

    private static final class Data {
        private boolean withSudo = false;
        private boolean asDaemon = false;
        private String redirectErr;
        private ExecBuilder.RedirectMode redirectModeErr;
        private String redirectOut;
        private ExecBuilder.RedirectMode redirectModeOut;
        private Map<String, String> environmentVariables = new HashMap<>();
        private CharSequenceTranslator argumentEscaper = ExecBuilder.DEFAULT_ESCAPE_ARG;
    }
}
