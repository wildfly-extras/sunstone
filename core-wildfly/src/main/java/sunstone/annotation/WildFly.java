package sunstone.annotation;



import sunstone.core.WildFlyConfig;
import sunstone.core.WildFlyDefault;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface WildFly {
    OperatingMode mode() default OperatingMode.STANDALONE;
    StandaloneMode standalone()  default @StandaloneMode();
    DomainMode domain()  default @DomainMode();
    public class WildFlyDefault implements WildFly {
        @Override
        public OperatingMode mode() {
            return OperatingMode.STANDALONE;
        }

        @Override
        public StandaloneMode standalone() {
            return new StandaloneDefault();
        }

        @Override
        public DomainMode domain() {
            return null;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return sunstone.annotation.WildFly.class;
        }

        static class StandaloneDefault implements StandaloneMode {
            @Override
            public String user() {
                return "${" + WildFlyConfig.MNGMT_USERNAME + "}";
            }

            @Override
            public String password() {
                return "${" + WildFlyConfig.MNGMT_PASSWORD + "}";
            }

            @Override
            public String port() {
                return "${" + WildFlyConfig.MNGMT_PORT + "}";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return StandaloneMode.class;
            }
        }
    }
}
