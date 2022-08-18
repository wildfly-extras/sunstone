package sunstone.api.inject;

import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.api.SunstoneResourceHint;

/**
 * Provides hostname of a resource.
 *
 * {@link SunstoneResourceHint#AWS_EC2_INSTANCE} - {@link Instance#publicIpAddress()}
 * {@link SunstoneResourceHint#AZ_VM_INSTANCE} - {@link PublicIpAddress#ipAddress()}
 * {@link SunstoneResourceHint#AZ_WEB_APP} - {@link WebApp#defaultHostname()}
 */
public interface Hostname {
    String get();
}
