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
 * Injected resource is based on field type, provided name and hint.
 * Supported resource injections for types:
 * <ul>
 *     <li>{@link OnlineManagementClient}: {@link SunstoneResource#resource()} and {@link SunstoneResource#hint()} is required
 *          <ul>
 *             <li>
 *                 {@link SunstoneResourceHint#AWS_EC2_INSTANCE}: {@link SunstoneResource#resource()} name of the instance ({@code Name} tag)
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_VM_INSTANCE}: {@link SunstoneResource#resource()} name of the instance
 *             </li>
 *          </ul>
 *     </li>
 *     <li>{@link Hostname}: {@link SunstoneResource#resource()} and {@link SunstoneResource#hint()} is required
 *          <ul>
 *             <li>
 *                 {@link SunstoneResourceHint#AWS_EC2_INSTANCE}: {@link SunstoneResource#resource()} name of the instance ({@code Name} tag)
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_VM_INSTANCE}: {@link SunstoneResource#resource()} name of the instance
 *             </li>
 *             <li>
 *                 {@link SunstoneResourceHint#AZ_WEB_APP}: {@link SunstoneResource#resource()} name of the instance
 *             </li>
 *          </ul>
 *     </li>
 *     <li>{@link S3Client}: {@link SunstoneResource#resource()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 *     <li>{@link Ec2Client}: {@link SunstoneResource#resource()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 *     <li>{@link AzureResourceManager}: {@link SunstoneResource#resource()} and {@link SunstoneResource#hint()} is NOT required
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SunstoneResource {
    /**
     * Resource identification for the injection. May be requiredFor more info see {@link SunstoneResource} JavaDoc.
     */
    String resource() default "";
    /**
     * Hint for resource identification. Sometimes resources don't have unique names. For more info see {@link SunstoneResource} JavaDoc.
     */
    SunstoneResourceHint hint() default SunstoneResourceHint.NONE;
}
