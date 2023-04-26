package sunstone.core.api;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.exceptions.SunstoneException;

public interface SunstoneCloudDeployer {
    void deployAndRegisterForUndeploy(ExtensionContext ctx) throws SunstoneException;
}
