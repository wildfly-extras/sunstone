package sunstone.core;


import sunstone.annotation.Parameter;
import sunstone.core.api.SunstoneCloudDeployer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static sunstone.core.SunstoneConfigResolver.resolveExpression;
import static sunstone.core.SunstoneConfigResolver.resolveOptionalExpression;

/**
 * Abstract class for providing common functionality for deploy operation to clouds. Tha class focuses on utilizing
 * work flow regarding getting resources, parameters and so on.
 *
 * Purpose: cloud specific deployers ought to extend this class and reuse the functionality.
 */
public abstract class AbstractSunstoneCloudDeployer implements SunstoneCloudDeployer {

    protected static Map<String, String> getParameters(Parameter[] parameters) {
        Map<String, String> parametersMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            parametersMap.put(parameters[i].k(), parameters[i].v());
        }
        parametersMap.forEach((key, value) ->  parametersMap.put(resolveExpression(key, String.class), resolveOptionalExpression(value, String.class).orElse("")));
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
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
