package org.wildfly.extras.sunstone.api.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.ConfigProperties;

import com.google.common.base.Strings;
import com.google.common.io.Resources;

/**
 * Object which holds an {@link ObjectPropertiesType}, a name and object properties.
 *
 */
public class ObjectProperties implements ConfigProperties {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    public static final String PROPERTIES_FILE_KEY = "properties";
    public static final String SYSTEM_PROPERTY_VALUE_DELIMITER = "clouds.sysprop.value.delimiter";
    public static final String SYSTEM_PROPERTY_VALUE_DELIMITER_DEFAULT = ":";

    private final Properties properties;
    private final ObjectPropertiesType objectType;
    private final String name;

    private final ObjectProperties template;

    public ObjectProperties(ObjectPropertiesType objectPropertiesType, String objectName) {
        this.properties = new Properties();
        this.objectType = objectPropertiesType;
        this.name = objectName;
        loadDefaults();
        final String templateName = getProperty(Config.TEMPLATE);
        if (!Strings.isNullOrEmpty(templateName)) {
            Set<String> nameSet = new HashSet<>();
            nameSet.add(name);
            template = new ObjectProperties(objectPropertiesType, templateName, nameSet);
        } else {
            template = null;
        }
    }

    /**
     * Ctor for templates
     */
    private ObjectProperties(ObjectPropertiesType objectPropertiesType, String name, Set<String> nameSet) {
        if (nameSet.contains(name)) {
            throw new IllegalArgumentException("Circular dependency in ObjectProperties template names");
        }
        this.properties = new Properties();
        this.objectType = objectPropertiesType;
        this.name = name;
        loadDefaults();
        final String templateName = getProperty(Config.TEMPLATE);
        if (!Strings.isNullOrEmpty(templateName)) {
            nameSet.add(name);
            template = new ObjectProperties(objectPropertiesType, templateName, nameSet);
        } else {
            template = null;
        }
    }

    public void applyOverrides(Map<String, String> overrides) {
        if (overrides != null) {
            LOGGER.trace("Applying ObjectProperties overrides: {}", overrides);
            properties.putAll(overrides);
        }
        LOGGER.debug("ObjectProperties for {} '{}': {}", objectType.getHumanReadableName(), name, properties);
    }

    public void addMissing(Map<String, String> overrides) {
        if (overrides != null) {
            LOGGER.trace("Applying missing ObjectProperties overrides: {}", overrides);
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                if (!properties.containsKey(entry.getKey())) {
                    properties.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        LOGGER.debug("ObjectProperties for {} '{}': {}", objectType.getHumanReadableName(), name, properties);
    }

    public String[] getArray(String propertyName, String splitRegex) {
        final String property = getProperty(propertyName, null);
        return property != null ? property.split(splitRegex) : null;
    }

    public String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    @Override
    public String toString() {
        return String.format("ObjectProperties [objectType=%s, name=%s, properties=%s]", objectType, name, properties);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((template == null) ? 0 : template.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ObjectProperties other = (ObjectProperties) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (objectType != other.objectType)
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (template == null) {
            if (other.template != null)
                return false;
        } else if (!template.equals(other.template))
            return false;
        return true;
    }

    public String getProperty(String propertyName, String defaultValue) {
        String result = System.getProperty(getSystemPropertiesKey(propertyName));
        if (result == null) {
            result = properties.getProperty(propertyName);
            if (result == null) {
                result = template != null ? template.getProperty(propertyName, defaultValue) : defaultValue;
            }
        }
        return replaceSystemProperties(result);
    }

    /**
     * Returns given property converted to integer. If the property is not configured or String-to-int conversion fails, then
     * default value (provided as the second parameter) is returned.
     */
    public int getPropertyAsInt(String propertyName, int defaultValue) {
        int result = defaultValue;
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null) {
            try {
                result = Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                LOGGER.debug("Property '{}' value '{}' is not a number, using default value '{}' instead", propertyName,
                        propertyValue, defaultValue);
            }
        }
        return result;
    }

    /**
     * Returns given node property converted to long. If the property is not configured or String-to-long conversion fails, then
     * default value (provided as the second parameter) is returned.
     */
    public long getPropertyAsLong(String propertyName, long defaultValue) {
        long result = defaultValue;
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null) {
            try {
                result = Long.parseLong(propertyValue);
            } catch (NumberFormatException e) {
                LOGGER.debug("Property '{}' value '{}' is not a number, using default value '{}' instead", propertyName,
                        propertyValue, defaultValue);
            }
        }
        return result;
    }

    public boolean getPropertyAsBoolean(String propertyName, boolean defaultValue) {
        boolean result = defaultValue;
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null) {
            result = Boolean.parseBoolean(propertyValue);
        }
        return result;
    }

    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * <p>Returns given property converted to a {@link Path}. The property value is interpreted as a filesystem path,
     * unless it begins with {@code classpath:}.</p>
     *
     * <p>If it <i>does</i> begin with {@code classpath:}, the rest of the value is interpreted as a path
     * to a classpath resource. The path must be absolute. As opposed to filesystem paths, absolute path
     * to a classpath resource must not begin with {@code /}. The classpath resource will be copied to a temporary file
     * that will be {@link File#deleteOnExit() deleted on JVM exit}. The filesystem path of that temporary file
     * will be returned.</p>
     *
     * <p>If the property is not configured, then default value (provided as the second parameter) is returned.</p>
     */
    public Path getPropertyAsPath(String propertyName, Path defaultValue) {
        String propertyValue = getProperty(propertyName);
        if (propertyValue == null) {
            return defaultValue;
        }

        if (propertyValue.startsWith(CLASSPATH_PREFIX)) {
            String pathOnClasspath = propertyValue.substring(CLASSPATH_PREFIX.length());

            if (pathOnClasspath.startsWith("/")) {
                throw new IllegalArgumentException("Classpath resource path shouldn't begin with '/': " + propertyValue);
            }

            URL classpathUrl = Resources.getResource(pathOnClasspath);

            // if the call to Resources.getResource() above succeeds, it also means that the path is sane
            int lastSlash = pathOnClasspath.lastIndexOf("/");
            String fileName = pathOnClasspath.substring(lastSlash < 0 ? 0 : lastSlash + 1);
            int lastPeriod = fileName.lastIndexOf(".");
            String prefix = fileName.substring(0, lastPeriod < 0 ? fileName.length() : lastPeriod);
            String suffix = lastPeriod < 0 ? null : fileName.substring(lastPeriod);

            try {
                Path tempFile = Files.createTempFile(prefix, suffix).toAbsolutePath().normalize();
                Resources.asByteSource(classpathUrl).copyTo(com.google.common.io.Files.asByteSink(tempFile.toFile()));
                tempFile.toFile().deleteOnExit();
                return tempFile;
            } catch (IOException e) {
                throw new IllegalArgumentException("Couldn't copy file from classpath to a temporary file: " + propertyValue, e);
            }
        }

        return Paths.get(propertyValue);
    }

    public String getSystemPropertiesKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        final StringBuilder sb = new StringBuilder(getKeyPrefix());
        sb.append(key);
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public ObjectPropertiesType getObjectType() {
        return objectType;
    }

    /**
     * Returns mutable Map instance with copy of properties. The system properties overrides are not used in the returned Map.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, String> toConfigMap() {
        return new HashMap<>((Map) properties);
    }

    private void loadDefaults() {
        final String keyPrefix = getKeyPrefix();
        final int prefixLen = keyPrefix.length();

        // read from CloudProperties first
        for (Entry<String, String> entry : CloudProperties.getInstance().getConfigMap().entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(keyPrefix)) {
                properties.setProperty(key.substring(prefixLen), entry.getValue());
            }
        }

        // check if we have an extra config file provided for this ObjectProperties instance
        final String systemPropertiesKey = getSystemPropertiesKey(PROPERTIES_FILE_KEY);
        final String defaultPropsPath = System.getProperty(systemPropertiesKey);
        if (Strings.isNullOrEmpty(defaultPropsPath)) {
            LOGGER.trace("Path to {} extra properties file was not configured (property '{}')",
                    objectType.getHumanReadableName(), systemPropertiesKey);
        } else {
            LOGGER.debug("Loading extra properties for {} '{}' from '{}'", objectType.getHumanReadableName(), name,
                    defaultPropsPath);
            try (final InputStream is = FilesUtils.openFile(defaultPropsPath)) {
                properties.load(is);
                LOGGER.debug("Loaded extra properties for {} '{}' from '{}'", objectType.getHumanReadableName(), name,
                        defaultPropsPath);
            } catch (IOException e) {
                LOGGER.debug("Failed loading extra properties for {} '{}' from '{}'",
                        objectType.getHumanReadableName(), name, defaultPropsPath, e);
            }
        }
    }

    private String getKeyPrefix() {
        final StringBuilder sb = new StringBuilder();
        if (objectType != null) {
            sb.append(objectType.getPropertyPrefix()).append(".");
        }
        if (null != Strings.emptyToNull(name)) {
            sb.append(name).append(".");
        }
        return sb.toString();
    }

    /**
     * Replaces all the occurrences of variables in the given source object with their matching values from the system
     * properties.
     *
     * @param source the source text containing the variables to substitute, null returns null
     * @return the result of the replace operation
     */
    public static String replaceSystemProperties(final Object source) {
        final StrSubstitutor strSubstitutor = new StrSubstitutor(StrLookup.systemPropertiesLookup());
        strSubstitutor.setValueDelimiter(
                System.getProperty(SYSTEM_PROPERTY_VALUE_DELIMITER, SYSTEM_PROPERTY_VALUE_DELIMITER_DEFAULT));
        return strSubstitutor.replace(source);
    }

}
