package sunstone.azure.annotation;


import sunstone.azure.impl.AzureConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify Azure PostgreSql Server.
 * <br>
 * name - required, name of the server
 * <br>
 * group - Optional, group to look in. By default {@code sunstone.azure.group} Sunstone Config property is used
 */
// represented by AzureIdentifiableSunstoneResource#VM_INSTANCE
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@AzureResourceIdentificationAnnotation
public @interface AzurePgSqlServer {
    String name();
    String group() default "${" + AzureConfig.GROUP + "}";
}
