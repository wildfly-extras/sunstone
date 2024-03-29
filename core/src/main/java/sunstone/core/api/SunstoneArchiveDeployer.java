package sunstone.core.api;

import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.exceptions.SunstoneException;

/**
 * Deployer service do deploy object. Modules providing such services determine scope of support.
 */
public interface SunstoneArchiveDeployer {
    void deploy(String deploymentName, Object deployment, ExtensionContext ctx) throws SunstoneException;
    void undeploy(String deploymentName, ExtensionContext ctx) throws SunstoneException;
}
