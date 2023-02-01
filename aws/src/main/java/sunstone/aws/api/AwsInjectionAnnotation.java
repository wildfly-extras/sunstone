package sunstone.aws.api;

import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.api.SunstoneInjectionAnnotation;
import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aggregates {@link SunstoneInjectionAnnotation} annotation for AWS module purposes.
 * <br>
 * Used to determine that the field has annotation marking AWS module ability to inject to the field.
 * <br>
 * This JavaDoc also aggregates information about what types can be injected by this AWS module and for what
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
 *         <th>Supported Aws identification annotations</th>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link OnlineManagementClient}
 *         </td>
 *         <td>
 *             {@link AwsEc2Instance}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link Hostname}
 *         </td>
 *         <td>
 *             {@link AwsEc2Instance}
 *             <br>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link Ec2Client}
 *         </td>
 *         <td>
 *             {@link AwsAutoResolve}
 *         </td>
 *         <td>
 *             {@link AwsRegion}
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             {@link S3Client}
 *         </td>
 *         <td>
 *             {@link AwsAutoResolve}
 *         </td>
 *         <td>
 *             {@link AwsRegion}
 *         </td>
 *     </tr>
 * </table>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@SunstoneInjectionAnnotation
public @interface AwsInjectionAnnotation {
}
