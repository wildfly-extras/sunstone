package sunstone.azure.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used for automatic resolution without providing any further details.
 */
// represented by AzureIdentifiableSunstoneResource#AUTO
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AzureResourceIdentificationAnnotation
public @interface AzureAutoResolve {
}
