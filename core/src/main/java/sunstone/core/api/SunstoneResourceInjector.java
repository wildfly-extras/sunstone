package sunstone.core.api;

import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.exceptions.SunstoneException;

/**
 * Service used by {@link sunstone.core.SunstoneExtension} to get object for an injection and register it if necessary.
 * <br>
 * Sunstone extension delegates managing resource's lifecycle
 * to the service meaning it should register closable resources to the extension store to be closed at proper time.
 */
public interface SunstoneResourceInjector {
    Object getResource(ExtensionContext ctx) throws SunstoneException;
    void closeResource(Object object) throws SunstoneException, Exception;
}
