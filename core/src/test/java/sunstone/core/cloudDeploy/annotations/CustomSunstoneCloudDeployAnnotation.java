package sunstone.core.cloudDeploy.annotations;

import sunstone.annotation.SunstoneCloudDeployAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SunstoneCloudDeployAnnotation
public @interface CustomSunstoneCloudDeployAnnotation {
}
