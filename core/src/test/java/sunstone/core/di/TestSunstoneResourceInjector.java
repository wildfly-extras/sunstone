package sunstone.core.di;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.SunstoneException;

import java.lang.annotation.Annotation;


public class TestSunstoneResourceInjector implements SunstoneResourceInjector {
    static boolean called;
    static boolean calledClose;
    static int counter = 0;

    public Object getResource(ExtensionContext ctx) {
        called = true;
        calledClose = true;
        counter++;
        return "set";
    }

    @Override
    public void closeResource(Object object) throws SunstoneException, Exception {
        calledClose = true;
    }

    public static void reset() {
        called = false;
        counter = 0;
    }
}
