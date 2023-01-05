package sunstone.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneCloudDeployer;

import java.lang.annotation.Annotation;

public class TestSunstoneDeployer implements SunstoneCloudDeployer {

    static boolean called = false;
    static int counter = 0;


    @Override
    public void deploy(Annotation clazz, ExtensionContext ctx) {
        called = true;
        counter++;
    }

    static void reset() {
        called = false;
        counter = 0;
    }
}
