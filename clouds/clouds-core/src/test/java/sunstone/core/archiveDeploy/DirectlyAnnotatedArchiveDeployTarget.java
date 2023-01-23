package sunstone.core.archiveDeploy;

import sunstone.api.SunstoneArchiveDeployTargetAnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SunstoneArchiveDeployTargetAnotation
public @interface DirectlyAnnotatedArchiveDeployTarget {
}
