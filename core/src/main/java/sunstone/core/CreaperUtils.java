package sunstone.core;


import org.slf4j.Logger;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import sunstone.annotation.StandaloneMode;
import sunstone.core.properties.ObjectProperties;
import sunstone.core.properties.ObjectType;

import java.io.IOException;

public class CreaperUtils {
    static Logger LOGGER = SunstoneLogger.DEFAULT;
    static ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);


    public static OnlineManagementClient createStandaloneManagementClient(String hostname, StandaloneMode standaloneMode) throws IOException {
        int port = Integer.parseInt(getResolvedOrGet(standaloneMode.port(), CloudsConfig.WildFly.MNGMT_PORT));
        String user = getResolvedOrGet(standaloneMode.user(), CloudsConfig.WildFly.MNGMT_USERNAME);
        String pass = getResolvedOrGet(standaloneMode.password(), CloudsConfig.WildFly.MNGMT_PASSWORD);
        int timeout = (int) TimeoutUtils.adjust(Integer.parseInt(objectProperties.getProperty(CloudsConfig.WildFly.MNGMT_CONNECTION_TIMEOUT)));
        LOGGER.debug("Creating management client {}:{} using credentials {}:{} with timeout {}", hostname, port, user, pass, timeout);
        OnlineOptions.OptionalOnlineOptions clientOptions = OnlineOptions.standalone()
                .hostAndPort(hostname, port)
                .auth(user, pass)
                .protocol(ManagementProtocol.HTTP_REMOTING)
                .connectionTimeout(timeout)
                .bootTimeout(timeout);
        return ManagementClient.online(clientOptions.build());
    }

    private static String getResolvedOrGet(String value, String objectPropertiesKey) {
        if (!value.isEmpty()) {
            return ObjectProperties.replaceSystemProperties(value);
        } else {
            return objectProperties.getProperty(objectPropertiesKey);
        }
    }
}
