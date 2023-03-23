package sunstone.core.archiveDeploy;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.exceptions.SunstoneException;

import java.io.InputStream;
import java.lang.annotation.Annotation;


public class TestSunstoneArchiveDeployer implements SunstoneArchiveDeployer {
    static boolean called;
    static boolean calledUndeploy;
    static int counter = 0;

    public static void reset() {
        called = false;
        calledUndeploy = false;
        counter = 0;
    }

    @Override
    public void deploy(String deploymentName, Object deployment, ExtensionContext ctx) throws SunstoneException {
        called = true;
        counter++;
    }

    @Override
    public void undeploy(String deploymentName, ExtensionContext ctx) throws SunstoneException {
        calledUndeploy = true;
    }
}
