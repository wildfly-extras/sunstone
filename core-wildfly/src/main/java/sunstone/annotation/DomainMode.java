package sunstone.annotation;

import sunstone.core.WildFlyConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_PARAMETER)
public @interface DomainMode {
    String host() default "${" + WildFlyConfig.MGMT_HOST + "}";
    String profile() default "${" + WildFlyConfig.MGMT_PROFILE + "}";
    String user() default "${" + WildFlyConfig.MGMT_USERNAME + "}";
    String password() default "${" + WildFlyConfig.MGMT_PASSWORD + "}";
    String port() default "${" + WildFlyConfig.MGMT_PORT + "}";
    //comma separated list
    String[] serverGroups() default "${" + WildFlyConfig.DOMAIN_SERVER_GROUPS + "}";
}
