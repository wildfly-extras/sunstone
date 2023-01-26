package azure.core.identification;

import com.azure.resourcemanager.AzureResourceManager;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.SunstoneInjectionAnnotation;
import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aggregates {@link SunstoneInjectionAnnotation} annotation for Azure module purposes.
 * <br>
 * Used to determine that the field has annotation marking Azure module ability to inject to the field.
 * <br>
 * This is for JavaDoc only. Aggregates information about what types can be injected by this Azure module and for what
 * cloud resources (identifiable by special annotation)
 * <br>
 * <br>
 * <b>All values in annotations are resolvable - ${my.system.property:default_value}. May be used more than once</b>
 * <br>
 * <br>
 *
 * Supported resource injections for field types:
 *
 * <table>
 *     <tr>
 *         <th>Type</th>
 *         <th>Supported Azure identification annotations</th>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link OnlineManagementClient}
 *         </td>
 *         <td>
 *             {@link AzureVirtualMachine}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link Hostname}
 *         </td>
 *         <td>
 *             {@link AzureVirtualMachine}
 *             <br>
 *             {@link AzureWebApplication}
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td>
 *             {@link AzureResourceManager}
 *         </td>
 *         <td>
 *             {@link AzureAutoResolve}
 *         </td>
 *     </tr>
 * </table>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@SunstoneInjectionAnnotation
public @interface AzureInjectionAnnotation {
}
