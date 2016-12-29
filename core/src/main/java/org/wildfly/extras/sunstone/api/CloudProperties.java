package org.wildfly.extras.sunstone.api;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.FilesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Singleton which manages cloud configuration properties. Use {@link #getConfigMap()} to retrieve current set of properties
 * (String key-value pairs).
 * <p>
 * By default the access to properties defined in {@link Config#DEFAULT_PROPERTIES} is provided. Other properties can be added
 * to this managed configuration by using any of <code>load(...)</code> methods.
 * </p>
 * <p>
 * Use {@link #reset()} to restore the default configuration.
 * </p>
 * <p>
 * Typical usage when you want to set a new configuration is:
 *
 * <pre>
 * CloudProperties.getInstance().reset().load("/path/to/a/new/cloud.properties");
 * </pre>
 *
 */
public final class CloudProperties {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    /**
     * Singleton instance holder.
     */
    private static final class Holder {
        static final CloudProperties INSTANCE = new CloudProperties();
    }

    private final Properties properties;

    private CloudProperties() {
        properties = new Properties();
        // ensure loading the defaults
        reset();
    }

    public static CloudProperties getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Resets this instance to default defined in {@link Config#DEFAULT_PROPERTIES}.
     */
    public CloudProperties reset() {
        LOGGER.debug("Resetting CloudProperties");
        synchronized (properties) {
            properties.clear();
            // reload default properties
            if (CloudProperties.class.getResource(Config.DEFAULT_PROPERTIES) != null) {
                try (InputStream is = CloudProperties.class.getResourceAsStream(Config.DEFAULT_PROPERTIES)) {
                    if (is != null) {
                        properties.load(is);
                        LOGGER.debug("CloudProperties reset to default settings from {}", Config.DEFAULT_PROPERTIES);
                    }
                } catch (IOException e) {
                    LOGGER.error("Unable to load default properties", e);
                }
            }
        }
        return this;
    }

    /**
     * Loads properties from given path (either system or classpath).
     *
     * @param path a property file path
     */
    public CloudProperties load(String path) {
        if (path != null) {
            LOGGER.debug("Loading CloudProperties from {}", path);
            try (final InputStream is = FilesUtils.openFile(path)) {
                if (is != null) {
                    properties.load(is);
                    LOGGER.debug("CloudProperties loaded from {}", path);
                } else {
                    String message = "Loading CloudProperties failed, because the file " + path + " was not found";
                    LOGGER.warn(message);
                    throw new ResourceLoadingException(message);
                }
            } catch (IOException e) {
                LOGGER.error("Loading CloudProperties from {} failed", path, e);
                throw new ResourceLoadingException(e);
            }
        } else {
            String message = "Unable to read properties from null path";
            LOGGER.warn(message);
            throw new NullPointerException(message);
        }
        return this;
    }

    /**
     * Loads properties from given {@link InputStream}. Closing the given {@code InputStream} is a responsibility
     * of the caller, this method keeps it open.
     */
    public CloudProperties load(InputStream is) {
        if (is != null) {
            LOGGER.debug("Loading CloudProperties from InputStream ({})", is);
            try {
                properties.load(is);
                LOGGER.debug("CloudProperties loaded from given InputStream", is);
            } catch (IOException e) {
                LOGGER.error("Loading CloudProperties from given InputStream failed", e);
                throw new ResourceLoadingException(e);
            }
        } else {
            String message = "Unable to read properties from null InputStream";
            LOGGER.warn(message);
            throw new NullPointerException(message);
        }
        return this;
    }

    /**
     * Loads properties for given class. It uses the provided class's classloader to load property file from resource which is
     * in the same package and has naming similar to Class name.
     * <br>
     * Samples (<code>cls.getName() -&gt; property file resource path<code>):
     * <pre>
     * "org.jboss.Test" -&gt; "/org/jboss/Test.properties"
     * "org.jboss.Test$Inner" -&gt; "/org/jboss/Test.Inner.properties"
     * </pre>
     *
     * @see #getPropertiesPathForClass(Class)
     */
    public CloudProperties load(Class<?> clazz) {
        if (clazz != null) {
            final String propertiesResourcePath = getPropertiesPathForClass(clazz);
            LOGGER.debug("Loading CloudProperties from classpath resource {}", propertiesResourcePath);
            try (InputStream is = clazz.getResourceAsStream(propertiesResourcePath)) {
                properties.load(is);
                LOGGER.debug("CloudProperties loaded from classpath resource {}", propertiesResourcePath);
            } catch (IOException e) {
                LOGGER.error("Loading CloudProperties from given classpath resource failed", e);
                throw new ResourceLoadingException(e);
            }
        } else {
            String message = "Unable to load properties for null Class";
            LOGGER.warn(message);
            throw new NullPointerException(message);
        }
        return this;
    }

    /**
     * Returns map of properties valid at the point of calling the.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, String> getConfigMap() {
        return ImmutableMap.copyOf((Map) properties);
    }


    /**
     * Helper method to retrieve name of property file, which is in the same package and has the same name as the given class.
     * If the given class is not top-level, then dots are used instead of dollar signs in final property file name.
     * <br>
     * Samples (<code>cls.getName()  - getPropertiesPathForClass(cls)</code>):
     * <pre>
     * "org.jboss.Test" - "/org/jboss/Test.properties"
     * "org.jboss.Test$Inner" - "/org/jboss/Test.Inner.properties"
     * </pre>
     *
     * @param clazz Class for which we want path to property-file
     * @return slash separated path to properties file located in the same package as the class
     */
    protected static String getPropertiesPathForClass(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Property file location can be retrieved for Class argument which is null.");
        final StringBuilder sb = new StringBuilder("/");
        final Package pkg = clazz.getPackage();
        final String className = clazz.getName().replaceAll("[$]", ".");
        if (pkg != null) {
            final String pkgName = pkg.getName();
            sb.append(pkgName.replaceAll("[.]", "/")).append("/");
            sb.append(className.substring(pkgName.length()+1));
        } else {
            sb.append(className);
        }
        sb.append(".properties");
        return sb.toString();
    }
}
