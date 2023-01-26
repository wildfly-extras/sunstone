package sunstone.core.di;


import org.junit.jupiter.api.extension.ExtensionContext;
import sunstone.core.api.SunstoneResourceInjector;

import java.lang.annotation.Annotation;


public class TestSunstoneResourceInjector implements SunstoneResourceInjector {
    static boolean called;
    static int counter = 0;

    public Object getAndRegisterResource(Annotation annotation, Class<?> fieldType, ExtensionContext ctx) {
        called = true;
        counter++;
        return "set";
    }

    public static void reset() {
        called = false;
        counter = 0;
    }
}
