package org.wildfly.extras.sunstone.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.FilesUtils;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;

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
                        loadAndProcess(is);
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
                    loadAndProcess(is);
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
                loadAndProcess(is);
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
                if (is == null) {
                    throw new ResourceLoadingException(String.format("Loading CloudProperties from classpath resource" +
                            " path %s (specified by %s) failed - check if your properties file exists", propertiesResourcePath, clazz));
                }
                loadAndProcess(is);
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

    /*
     * Handles inheritance transformation from {@code templateTo} to {@code template}.
     */
    private void loadAndProcess(InputStream is) throws IOException {
        Set<String> definedNodes = getDefinedNodeNames();
        properties.load(is);
        Set<String> newDefinedNodes = getDefinedNodeNames();
        newDefinedNodes.removeAll(definedNodes);
        for (String node : newDefinedNodes) {
            addReverseMapping(node);
        }
    }

    /*
     * Returns the list of names of nodes taken from the {@code node.<nodeName>...} properties.
     */
    private Set<String> getDefinedNodeNames() {
        Set<String> definedNodes = new HashSet<>();
        for (String propKey : properties.stringPropertyNames()) {
            if (!propKey.startsWith(ObjectType.NODE.getPropertyPrefix())) continue; // only interested in "node... properties"
            String[] propertyNameSegments = propKey.split("\\.");
            if (propertyNameSegments.length <= 1) continue; // looks like just a "node..." property - unrecognized, but not an error
            definedNodes.add(propertyNameSegments[1]);
        }
        return definedNodes;
    }

    /*
     * Adds {@code template} properties for each node in {@code templateTo} property of the specified node.
     */
    private void addReverseMapping(String node) {
        String value = properties.getProperty(String.format("%s.%s.%s", ObjectType.NODE.getPropertyPrefix(), node, Config.TEMPLATE_TO));
        if (value == null) return; // not found, this node has no templateTo
        String[] copies = value.split(",");
        for (String copy : copies) {
            String newProp = String.format("%s.%s.%s", ObjectType.NODE.getPropertyPrefix(), copy, Config.TEMPLATE);
            if (null != properties.getProperty(newProp) && !node.equals(properties.getProperty(newProp))) {
                throw new IllegalStateException(String.format("Node %s already has a template: %s, cannot add a conflicting template %s",
                        copy, properties.getProperty(newProp), node));
            }
            properties.setProperty(newProp, node);
        }
    }

    /**
     * @param node the name of the node whose list of copies we want
     * @return a set of names of the nodes which inherit via {@code template} or {@code templateTo} from the given node
     */
    public static Set<String> getSetOfCopies(String node) {
        if (Strings.isNullOrEmpty(node)) {
            throw new IllegalArgumentException("Cannot return a set of copies for an empty node");
        }

        Set<String> result = new HashSet<>();
        CloudProperties cp = CloudProperties.getInstance();
        for (String propKey : cp.properties.stringPropertyNames()) {
            if (!propKey.startsWith(ObjectType.NODE.getPropertyPrefix())) continue;
            if (propKey.split("\\.").length <= 1) continue;
            String nodeName = propKey.split("\\.")[1];
            if (node.equals(nodeName)) { // this might be a templateTo link, add all of its values
                String templateToValue = cp.properties.getProperty(String.format("%s.%s.%s",
                        ObjectType.NODE.getPropertyPrefix(), nodeName, Config.TEMPLATE_TO));
                if (templateToValue != null) {
                    result.addAll(Arrays.asList(templateToValue.split(",")));
                }
            } else { // this might be a template link, add the name of the node
                String templateValue = cp.properties.getProperty(String.format("%s.%s.%s",
                        ObjectType.NODE.getPropertyPrefix(), nodeName, Config.TEMPLATE));
                if (templateValue != null && node.equals(templateValue)) {
                    result.add(nodeName);
                }
            }
        }
        return result;
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
        Objects.requireNonNull(clazz, "Property file location cannot be retrieved for Class argument which is null.");
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
