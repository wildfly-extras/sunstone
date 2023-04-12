package sunstone.core.cloudDeploy;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneCloudDeployer;

public class TestSunstoneDeployer implements SunstoneCloudDeployer {

    static boolean called = false;


    @Override
    public void deployAndRegisterForUndeploy(ExtensionContext ctx) {
        called = true;
    }

    static void reset() {
        called = false;
    }
}
