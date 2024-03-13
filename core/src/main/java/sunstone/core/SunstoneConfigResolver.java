package sunstone.core;


import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The class wraps and configure SmallRyeConfig so that it can resolve bare expressions and read sunstone.properties
 */
public class SunstoneConfigResolver {

    static final SmallRyeConfig config = new SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .addDefaultSources()
            .addDiscoveredSources()
            .withSources(new SunstonePropertiesSource(), new SunstoneExpressionSource())
            .build();

    /**
     * Get property defined by the key and resolve it to the String.
     */
    public static String getString(String key) {
        return config.getValue(key, String.class);
    }

    public static SmallRyeConfig unwrap() {
        return config;
    }

    /**
     * Return the resolved property value with the specified type for the specified property name from the underlying configuration sources.
     * Delegate to {@link SmallRyeConfig#getValue(String, Class)} to get a propety
     */
    public static <V> V getValue(String key, Class<V> clazz) {
        return config.getValue(key, clazz);
    }

    /**
     * Return the resolved property value with the specified type (default value) for the specified property name from the underlying configuration sources.
     * If not present, return default value. Type of the default value is also used as
     * a converter in {@link SmallRyeConfig#getValue(String, Class)}
     */
    @SuppressWarnings("unchecked")
    public static <V> V getValue(String key, V defaultValue) {
        Objects.requireNonNull(defaultValue);
        Object value = config.isPropertyPresent(key) ? config.getValue(key, defaultValue.getClass()) : defaultValue;
        if (!defaultValue.getClass().isAssignableFrom(value.getClass())) {
            throw new ClassCastException(String.format("Can not cast %s to %s", value, defaultValue.getClass()));
        }
        return (V) defaultValue.getClass().cast(value);
    }

    /**
     * This method resolves expression. Use case is not value from key=value which is handled by {@link #getString(String)},
     * {@link #getValue(String, Class)} or {@link #getValue(String, Object)}. The usecase is:
     * <ol>
     *     <li>
     *         {@code my.property} is defined in some other configuration source:
     *         <ul>
     *             <li>{@code my.property=1} in {@code sunstone.properties}</li>
     *             <li>or {@code my.property=1} as environment variable</li>
     *             <li>or {@code my.property=1} as system property</li>
     *         </ul>
     *     </li>
     *     <li>
     *         A class is annotated with {@code @Annotation("name ${my.property}")}
     *     </li>
     *     <li>
     *         You use this method to resolve {@code "name ${my.property}"} to {@code "name 1"}"
     *     </li>
     * </ol>
     * <p>
     * Since SR Config is used, all capabilities of the library is supported (nested exprssions, ... )
     * <p>
     * Parameter {@code clazz} defines a type of the returned value. I.e. {@code "10${my.property}"} may be resolved to
     * integer {@code 101} or string {@code "101"}, ...
     */
    public static <V> V resolveExpression(String expression, Class<V> clazz) {
        return config.getValue(SunstoneExpressionSource.registerExpression(expression), clazz);
    }

    public static <V> Optional<V> resolveOptionalExpression(String expression, Class<V> clazz) {
        return config.getOptionalValue(SunstoneExpressionSource.registerExpression(expression), clazz);
    }


    /**
     *
     * This method resolves expression to String. Usecase:
     * <ol>
     *     <li>
     *         {@code my.property} is defined in other configuration source:
     *         <ul>
     *             <li>{@code my.property=1} in {@code sunstone.properties}</li>
     *             <li>{@code my.property=1} as environment variable</li>
     *             <li>{@code my.property=1} as system property</li>
     *         </ul>
     *     </li>
     *     <li>
     *         A class is annotated with {@code @Annotation("name ${my.property}")}
     *     </li>
     *     <li>
     *         You use this method to resolve {@code "name ${my.property}"} to {@code "name 1"}"
     *     </li>
     * </ol>
     * <p>
     * Since SR Config is used, all capabilities of the library is supported (nested exprssions, ... )
     * <p>
     * If expression is blank, it is returned as is.
     *
     */
    public static String resolveExpressionToString(String expression) {
        return resolveExpression(expression, String.class);
    }

    /**
     * Returns true if string contains ${.*}
     */
    public static boolean isExpression(String candidate) {
        return candidate.matches("^.*\\$\\{.*\\}.*$");
    }

    /**
     * Source for sunstone.properties.
     */
    static class SunstonePropertiesSource implements ConfigSource {
        public static final String DEFAULT_PROPERTIES = "/sunstone.properties";
        public Properties load() {
            Properties properties = new Properties();
            // reload default properties
            if (SunstonePropertiesSource.class.getResource(SunstonePropertiesSource.DEFAULT_PROPERTIES) != null) {
                try (InputStream is = SunstonePropertiesSource.class.getResourceAsStream(SunstonePropertiesSource.DEFAULT_PROPERTIES)) {
                    if (is != null) {
                        properties.load(is);
                    }
                } catch (IOException e) {
                    SunstoneLogger.DEFAULT.error("Unable to load default properties", e);
                }
            }
            return properties;
        }

        @Override
        public Set<String> getPropertyNames() {
            return load().stringPropertyNames();
        }

        @Override
        public String getValue(String propertyName) {
            return load().getProperty(propertyName);
        }

        @Override
        public String getName() {
            return "sunstone-source";
        }
    }

    /**
     * Fake SR Config source for resolving expressions.
     * To resolve expression, you need to:
     *  - Register {@link SunstoneExpressionSource} as a source for SR Config
     *  - SR Config must be created with correct interceptors so that expressions are resolver
     *  - register expression as a new property in internal map and get key to retrieve it - {@link #registerExpression(String)}
     *  - retrieve the value ({@link SmallRyeConfig#getValue(String, Class)}) using key provided by {@link #registerExpression(String)}
     */
    static class SunstoneExpressionSource implements ConfigSource {

        static final ConcurrentMap<String, String> expressions = new ConcurrentHashMap<>();

        static String registerExpression(String expression) {
            String key = "sunstone-expression-" + expression;
            expressions.putIfAbsent(key, expression);
            return key;
        }

        @Override
        public Set<String> getPropertyNames() {
            return expressions.keySet();
        }

        @Override
        public String getValue(String s) {
            return expressions.get(s);
        }

        @Override
        public String getName() {
            return "sunstone-expression-source";
        }
    }
}
