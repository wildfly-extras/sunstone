package sunstone.core;


import com.google.common.io.BaseEncoding;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import sunstone.api.Parameter;
import sunstone.core.api.SunstoneCloudDeployer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for providing common functionality for deploy operation to clouds. Tha class focuses on utilizing
 * work flow regarding getting resources, parameters and so on.
 *
 * Purpose: cloud specific deployers ought to extend this class and reuse the functionality.
 */
public abstract class AbstractSunstoneCloudDeployer implements SunstoneCloudDeployer {

    protected static String md5sum(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(str.getBytes("UTF-8"));
        byte[] digest = sha256.digest();
        return BaseEncoding.base16().encode(digest);
    }
    protected static String resolveOrGetFromSunstoneProperties(String toResolve, String sunstoneProperty) {
        String resolved = null;
        if (!toResolve.isEmpty()) {
            resolved = ObjectProperties.replaceSystemProperties(toResolve);
        } else if (sunstoneProperty != null) {
            ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUDS, null);
            resolved = objectProperties.getProperty(sunstoneProperty);
        }
        return resolved;
    }

    protected static Map<String, String> getParameters(Parameter[] parameters) {
        Map<String, String> parametersMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            parametersMap.put(parameters[i].k(), parameters[i].v());
        }
        parametersMap.forEach((key, value) -> parametersMap.put(key, ObjectProperties.replaceSystemProperties(value)));
        return Collections.unmodifiableMap(parametersMap);
    }

    protected static String getResourceContent(String resource) throws IOException {
        ByteArrayOutputStream result;
        try (InputStream is = AbstractSunstoneCloudDeployer.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalArgumentException("Can not find resource " + resource);
            }
            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = is.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}