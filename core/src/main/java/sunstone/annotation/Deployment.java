package sunstone.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate method to automate WildFly deploy operation.
 * <br>
 * Deployment operation is done before static resources are injected.
 * <br>
 * The method also needs an annotation annotated with {@link SunstoneArchiveDeployTargetAnotation} that marks an annotation
 * which is used to identify Cloud resource, i.e. virtual machine. Those annotations bring modules like sunstone-clouds-azure.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface Deployment {
    /**
     * Name of the deployment. Some resources may not support the name and will ignore it.
     * <br>
     * For example Azure App services - the way Azure platform works, it is always deployed as ROOT.war
     */
    String name() default "";
}
