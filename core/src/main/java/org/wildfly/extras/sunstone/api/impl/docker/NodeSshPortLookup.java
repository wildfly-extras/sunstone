package org.wildfly.extras.sunstone.api.impl.docker;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.jclouds.docker.compute.functions.LoginPortForContainer;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.HostConfig;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.Config;

import java.util.List;
import java.util.Map;

/**
 * This class helps to inform JClouds Docker provider about SSH port used in the Node. Port number used is read from Containers
 * environment variable {@value Config.Node.Docker#ENV_NAME_SSH_PORT}.
 *
 */
public class NodeSshPortLookup implements LoginPortForContainer {
    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    @Override
    public Optional<Integer> apply(Container container) {
        final List<String> environmentVariables = container.config().env();
        if (environmentVariables == null) {
            return Optional.<Integer>absent();
        }
        Optional<String> port = Iterables.tryFind(environmentVariables, environmentVariable -> {
            String[] var = environmentVariable.split("=");
            return Config.Node.Docker.ENV_NAME_SSH_PORT.equals(var[0]);
        });
        if (port.isPresent()) {
            final String portStr = port.get().split("=")[1];
            try {
                final int portNumber = Integer.parseInt(portStr);
                return Optional.fromNullable(getPublicTcpPort(portNumber, container.hostConfig()));
            } catch (NumberFormatException e) {
                LOGGER.warn("Docker container environment variable '{}' value '{}' is not a number",
                        Config.Node.Docker.ENV_NAME_SSH_PORT , portStr);
            }
        }
        return Optional.<Integer> absent();
    }

    private static Integer getPublicTcpPort(int tcpPort, HostConfig hostConfig) {
        Integer result = null;
        if ("host".equals(hostConfig.networkMode())) {
            result = tcpPort;
        } else {
            final String portName = tcpPort + "/tcp";
            final Map<String, List<Map<String, String>>> allPortBindings = hostConfig.portBindings();
            if (allPortBindings != null) {
                final List<Map<String, String>> singlePortBindings = allPortBindings.get(portName);
                if (singlePortBindings != null && !singlePortBindings.isEmpty()) {
                    String hostPort = singlePortBindings.get(0).get("HostPort");
                    if (!Strings.isNullOrEmpty(hostPort)) {
                        try {
                            result = Integer.valueOf(hostPort);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Port binding value '{}' for port '{}' is not a number", hostPort, portName);
                        }
                    }
                } else {
                    LOGGER.debug("Unable to find TCP port mapping for SSH port {}", tcpPort);
                }
            }
        }
        return result;
    }
}
