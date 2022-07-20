package sunstone.api;

import org.junit.jupiter.api.extension.ExtendWith;
import sunstone.core.SunstoneExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@ExtendWith({SunstoneExtension.class})
public @interface Sunstone {
}
