package sunstone.azure.api;

import com.azure.resourcemanager.appservice.models.WebApp;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.SunstoneArchiveDeployTargetAnotation;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Aggregates {@link SunstoneArchiveDeployTargetAnotation} annotation for Azure module purposes.
 * <br>
 * Used to determine that the method annotated by {@link sunstone.api.Deployment} has annotation marking Azure module ability
 * to deploy to the resource.
 * <br>
 * This is for JavaDoc only. Aggregates information about what resources are supported for archive (JAR, WAR, EAR) deployment.
 * <br>
 * <br>
 * <b>All values in annotations are resolvable - ${my.system.property:default_value}. May be used more than once</b>
 * <br>
 * <br>
 *
 * Supported resources:
 * <table>
 *     <tr>
 *         <th>Supported Azure identification annotations</th>
 *         <th>notes</th>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link AzureVirtualMachine}
 *         </td>
 *         <td>
 *             {@link OnlineManagementClient} client is used and {@link Deploy} is used. {@link Undeploy} is run on after all callback.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link AzureWebApplication}
 *         </td>
 *         <td>
 *             Azure SDK {@link WebApp#warDeploy(File)} is used. The module restart the app and waits until welcome page is gone.
 *             Undeploy operation: purge directory {@code /site/wwwroot/}, restarts the app and waits for the welcome page
 *             <br>
 *         </td>
 *     </tr>
 * </table>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@SunstoneArchiveDeployTargetAnotation
public @interface AzureArchiveDeploymentAnnotation {
}
