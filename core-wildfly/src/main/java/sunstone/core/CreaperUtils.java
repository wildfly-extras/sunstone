package sunstone.core;


import org.slf4j.Logger;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import sunstone.annotation.DomainMode;
import sunstone.annotation.StandaloneMode;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static sunstone.core.SunstoneConfig.getString;
import static sunstone.core.SunstoneConfig.getValue;
import static sunstone.core.SunstoneConfig.isExpression;
import static sunstone.core.SunstoneConfig.resolveExpressionToString;

public class CreaperUtils {
    static Logger LOGGER = WildFlyLogger.DEFAULT;

    public static OnlineManagementClient createStandaloneManagementClient(String hostname, StandaloneMode standaloneMode) throws IOException, SunstoneException {

        try {
            int port = isExpression(standaloneMode.port()) ? SunstoneConfig.resolveExpression(standaloneMode.port(), Integer.class) : Integer.parseInt(standaloneMode.port());
            String user = isExpression(standaloneMode.user()) ? resolveExpressionToString(standaloneMode.user()) : standaloneMode.user();
            String pass = isExpression(standaloneMode.password()) ? resolveExpressionToString(standaloneMode.password()) : standaloneMode.password();
            int timeout = (int) TimeoutUtils.adjust(getValue(WildFlyConfig.MGMT_CONNECTION_TIMEOUT, 120000));
            LOGGER.debug("Creating management client {}:{} using credentials {}:{} with timeout {}", hostname, port, user, pass, timeout);
            OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.standalone()
                    .hostAndPort(hostname, port)
                    .auth(user, pass)
                    .protocol(ManagementProtocol.HTTP_REMOTING)
                    .connectionTimeout(timeout)
                    .bootTimeout(timeout);
            return ManagementClient.online(clientOptions.build());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentSunstoneException("Port is not a number.", e);
        }
    }

    public static OnlineManagementClient createDomainManagementClient(String hostname, DomainMode domainMode) throws IOException, SunstoneException {
        try {
            int port = isExpression(domainMode.port()) ? SunstoneConfig.resolveExpression(domainMode.port(), Integer.class) : Integer.parseInt(domainMode.port());
            String user = isExpression(domainMode.user()) ? resolveExpressionToString(domainMode.user()) : getString(domainMode.user());
            String pass = isExpression(domainMode.password()) ? resolveExpressionToString(domainMode.password()) : domainMode.password();
            int timeout = (int) TimeoutUtils.adjust(getValue(WildFlyConfig.MGMT_CONNECTION_TIMEOUT, 120000));
            String host = isExpression(domainMode.host()) ? resolveExpressionToString(domainMode.host()) : domainMode.host();
            String profile = isExpression(domainMode.profile()) ? resolveExpressionToString(domainMode.profile()) : domainMode.profile();
            LOGGER.debug("Creating management client {}:{} with profile {}:{} using credentials {}:{} with timeout {}", hostname, port, host, profile, user, pass, timeout);

            OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.domain()
                    .forProfile(profile)
                    .forHost(host)
                    .build()
                    .hostAndPort(hostname, port)
                    .auth(user, pass)
                    .protocol(ManagementProtocol.HTTP_REMOTING)
                    .connectionTimeout(timeout)
                    .bootTimeout(timeout);
            return ManagementClient.online(clientOptions.build());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentSunstoneException("Port is not a number.", e);
        }
    }

    public static void setDomainServers(Deploy.Builder builder, DomainMode domainMode) {
        if (domainMode == null || domainMode.serverGroups() == null) {
            throw new RuntimeException(WildFlyConfig.DOMAIN_SERVER_GROUPS + " is not set");
        }
        String[] serverGroupsParams = domainMode.serverGroups();
        boolean deployedToNone = true;

        for (String sgParam : serverGroupsParams) {
            Optional<String[]> serverGroups = isExpression(sgParam) ? SunstoneConfig.resolveOptionalExpression(sgParam, String[].class) : Optional.of(new String[]{sgParam});
            if (serverGroups.isPresent()) {
                deployedToNone = false;
                serverGroups.ifPresent(groups -> Arrays.stream(groups).forEach(builder::toServerGroups));
            }
        }
        //groups may not be set -> deploy to all groups
        if (deployedToNone) {
            builder.toAllServerGroups();
        }
    }
}
