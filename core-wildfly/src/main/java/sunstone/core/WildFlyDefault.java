package sunstone.core;


import sunstone.annotation.DomainMode;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.StandaloneMode;
import sunstone.annotation.WildFly;

import java.lang.annotation.Annotation;

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
