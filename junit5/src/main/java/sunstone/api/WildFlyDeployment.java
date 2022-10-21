package sunstone.api;

import org.jboss.shrinkwrap.api.Archive;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

/**
 * Annotate method to automate WildFly deploy operation.
 * Deployment operation is done after {@link Setup}.
 *
 * Supported method return types:
 * <ul>
 *     <li> {@link Path}</li>
 *     <li> {@link File}</li>
 *     <li> {@link Archive}</li>
 *     <li> {@link InputStream}</li>
 * </ul>
 *
 * Supported resources Sunstone is able to deploy to:
 * <ul>
 *    <li>
 *        {@link SunstoneResourceHint#AWS_EC2_INSTANCE}: {@link #to()} name of the instance ({@code Name} tag)
 *    </li>
 *    <li>
 *        {@link SunstoneResourceHint#AZ_VM_INSTANCE}: {@link #to()} name of the instance
 *    </li>
 *    <li>
 *        {@link SunstoneResourceHint#AZ_WEB_APP}: {@link #to()} name of the instance.
 *        <br>
 *        <b>Azure App services can deploy only WAR archive</b>
 *        <br>
 *        Be aware {@link #name()} doesn't matter - AzureSDK always deploys the archive as ROOT.war. Deployment isn't
 *        un-deployed after the test. If different archive is deployed, the old one is simply overridden and container is
 *        restarted.
 *    </li>
 * </ul>
 *
 * Supported datatypes of method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface WildFlyDeployment {
    String name() default "";
    String to();
    SunstoneResourceHint hint();
}
