package sunstone.core.archiveDeploy;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.exceptions.SunstoneException;

import java.io.InputStream;
import java.lang.annotation.Annotation;


public class TestSunstoneArchiveDeployer implements SunstoneArchiveDeployer {
    static boolean called;
    static int counter = 0;

    public static void reset() {
        called = false;
        counter = 0;
    }

    @Override
    public void deployAndRegisterUndeploy(String deploymentName, Annotation targetAnnotation, InputStream deployment, ExtensionContext ctx) throws SunstoneException {
        called = true;
        counter++;
    }
}
