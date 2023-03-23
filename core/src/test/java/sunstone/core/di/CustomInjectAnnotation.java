package sunstone.core.di;

import sunstone.annotation.CloudResourceIdentificationAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@CloudResourceIdentificationAnnotation
public @interface CustomInjectAnnotation {
}
