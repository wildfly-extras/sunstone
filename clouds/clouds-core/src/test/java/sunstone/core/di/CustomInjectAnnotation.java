package sunstone.core.di;

import sunstone.api.SunstoneInjectionAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SunstoneInjectionAnnotation
public @interface CustomInjectAnnotation {
}
