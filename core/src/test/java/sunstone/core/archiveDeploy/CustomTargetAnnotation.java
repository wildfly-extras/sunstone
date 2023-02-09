package sunstone.core.archiveDeploy;

import sunstone.annotation.SunstoneArchiveDeployTargetAnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SunstoneArchiveDeployTargetAnotation
public @interface CustomTargetAnnotation {
}
