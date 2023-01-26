package sunstone.core.properties;

import java.nio.file.Path;

/**
 * Read-only access to configuration properties of given object, such as {@link Node} or {@link CloudProvider}.
 */
public interface ConfigProperties {
    /** Returns the string value of given configuration property, or {@code null} if the property doesn't exist. */
    String getProperty(String propertyName);

    /**
     * Returns the string value of given configuration property, or {@code defaultValue} if the property doesn't exist.
     */
    String getProperty(String propertyName, String defaultValue);

    /**
     * Returns the integer ({@code int}) value of given configuration property, or {@code defaultValue} if the property
     * doesn't exist. If the property value is not an integer, {@code defaultValue} is returned as well.
     */
    int getPropertyAsInt(String propertyName, int defaultValue);

    /**
     * Returns the integer ({@code long}) value of given configuration property, or {@code defaultValue} if the property
     * doesn't exist. If the property value is not an integer, {@code defaultValue} is returned as well.
     */
    long getPropertyAsLong(String propertyName, long defaultValue);

    /**
     * Returns the boolean value of given configuration property, or {@code defaultValue} if the property doesn't exist.
     * Uses {@link Boolean#parseBoolean(String)} for parsing the property value.
     */
    boolean getPropertyAsBoolean(String propertyName, boolean defaultValue);

    /**
     * <p>Returns given configuration property as a {@link Path}. The property value is interpreted as a filesystem
     * path, unless it begins with {@code classpath:}.</p>
     *
     * <p>If it <i>does</i> begin with {@code classpath:}, the rest of the value is interpreted as a path
     * to a classpath resource. The path must be absolute. As opposed to filesystem paths, absolute path
     * to a classpath resource must not begin with {@code /}. The classpath resource will be copied to a temporary file
     * that will be {@link java.io.File#deleteOnExit() deleted on JVM exit}. The filesystem path of that temporary file
     * will be returned.</p>
     *
     * <p>If the property is not configured, then default value is returned.</p>
     */
    Path getPropertyAsPath(String propertyName, Path defaultValue);
}
