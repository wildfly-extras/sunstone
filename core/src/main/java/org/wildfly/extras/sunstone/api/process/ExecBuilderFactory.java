package org.wildfly.extras.sunstone.api.process;

import java.util.Objects;

/**
 * Factory class that allows to create preconfigured {@link ExecBuilder}
 */
public final class ExecBuilderFactory {
    private final ExecBuilderOptions options;

    /**
     * Constructor
     *
     * @param properties configuration used to initialize {@link ExecBuilder}
     */
    public ExecBuilderFactory(ExecBuilderOptions properties) {
        this.options = properties;
    }

    /**
     * Creates instance of {@link ExecBuilder} from the given command.
     *
     * @param command mandatory command with optional arguments
     */
    public ExecBuilder fromCommand(String... command) {
        return new ExecBuilder(options, command);
    }

    /**
     * Creates instance of {@link ExecBuilder} from the given shell script.
     *
     * @param script shell script to be executed (must be not-{@code null})
     */
    public ExecBuilder fromShellScript(String script) {
        return new ExecBuilder(options, "sh", "-c", Objects.requireNonNull(script, "Shell script has to be provided"));
    }

}
