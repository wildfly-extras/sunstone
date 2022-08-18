package sunstone.api;

import com.azure.resourcemanager.AzureResourceManager;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.api.inject.Hostname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supported resource injections:
 * <ul>
 *     <li>{@link OnlineManagementClient}: {@link SunstoneResource#of()} and {@link SunstoneResource#hint()} is required
 *          <ul>
 *             <li>
 *                 {@link SunstoneResourceHint#AWS_EC2_INSTANCE}: {@link SunstoneResource#of()} name of the instance ({@code Name} tag)
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_VM_INSTANCE}: {@link SunstoneResource#of()} name of the instance
 *             </li>
 *          </ul>
 *     </li>
 *     <li>{@link Hostname}: {@link SunstoneResource#of()} and {@link SunstoneResource#hint()} is required
 *          <ul>
 *             <li>
 *                 {@link SunstoneResourceHint#AWS_EC2_INSTANCE}: {@link SunstoneResource#of()} name of the instance ({@code Name} tag)
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_VM_INSTANCE}: {@link SunstoneResource#of()} name of the instance
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_WEB_APP}: {@link SunstoneResource#of()} name of the instance
 *             </li>
 *          </ul>
 *     </li>
 *     <li>{@link S3Client}: {@link SunstoneResource#of()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 *     <li>{@link Ec2Client}: {@link SunstoneResource#of()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 *     <li>{@link AzureResourceManager}: {@link SunstoneResource#of()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SunstoneResource {
    String of() default "";
    SunstoneResourceHint hint() default SunstoneResourceHint.NONE;
}
