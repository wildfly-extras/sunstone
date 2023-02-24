package sunstone.core;


import org.slf4j.Logger;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import sunstone.annotation.StandaloneMode;

import java.io.IOException;

public class CreaperUtils {
    static Logger LOGGER = SunstoneLogger.DEFAULT;

    public static OnlineManagementClient createStandaloneManagementClient(String hostname, StandaloneMode standaloneMode) throws IOException {

        int port = standaloneMode.port().isBlank() ? SunstoneConfig.getValue(ConfigProperties.WildFly.MNGMT_PORT, Integer.class) : SunstoneConfig.resolveExpression(standaloneMode.port(), Integer.class);
        String user = standaloneMode.user().isBlank() ? SunstoneConfig.getString(ConfigProperties.WildFly.MNGMT_USERNAME) : SunstoneConfig.resolveExpressionToString(standaloneMode.user());
        String pass = standaloneMode.password().isBlank() ? SunstoneConfig.getString(ConfigProperties.WildFly.MNGMT_PASSWORD) : SunstoneConfig.resolveExpressionToString(standaloneMode.password());
        int timeout = (int) TimeoutUtils.adjust(SunstoneConfig.getValue(ConfigProperties.WildFly.MNGMT_CONNECTION_TIMEOUT, 120000));
        LOGGER.debug("Creating management client {}:{} using credentials {}:{} with timeout {}", hostname, port, user, pass, timeout);
        OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.standalone()
                .hostAndPort(hostname, port)
                .auth(user, pass)
                .protocol(ManagementProtocol.HTTP_REMOTING)
                .connectionTimeout(timeout)
                .bootTimeout(timeout);
        return ManagementClient.online(clientOptions.build());
    }
}
