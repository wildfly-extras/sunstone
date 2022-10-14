package sunstone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
@Repeatable(WithJCloudWildFlyNodesRepetable.class)
public @interface WithJCloudWildFlyNodes {
    String value();
    ValueType type() default ValueType.RESOURCE;
    String[] nodes() default "";
}
