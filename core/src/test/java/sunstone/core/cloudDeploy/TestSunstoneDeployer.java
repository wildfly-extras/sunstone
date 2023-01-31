package sunstone.core.cloudDeploy;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneCloudDeployer;

import java.lang.annotation.Annotation;

public class TestSunstoneDeployer implements SunstoneCloudDeployer {

    static boolean called = false;


    @Override
    public void deploy(Annotation clazz, ExtensionContext ctx) {
        called = true;
    }

    static void reset() {
        called = false;
    }
}
