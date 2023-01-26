package sunstone.core.properties;


/**
 * Interface which allows to introduce types for {@link ObjectProperties} instances.
 *
 */
public interface ObjectPropertiesType {

    /**
     * Returns property prefix for this type. It's used in {@link CloudProperties}.
     *
     * @return
     */
    String getPropertyPrefix();

    /**
     * Returns human readable/understandable representation of this type.
     *
     * @return
     */
    String getHumanReadableName();
}
