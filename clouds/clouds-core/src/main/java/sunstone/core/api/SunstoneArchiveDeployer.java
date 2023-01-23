package sunstone.core.api;

import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.exceptions.SunstoneException;

import java.io.InputStream;
import java.lang.annotation.Annotation;

/**
 *
 */
public interface SunstoneArchiveDeployer {
    void deployAndRegisterUndeploy(String deploymentName, Annotation targetAnnotation, InputStream deployment, ExtensionContext ctx) throws SunstoneException;
}
