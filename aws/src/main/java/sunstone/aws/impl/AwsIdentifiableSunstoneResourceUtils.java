package sunstone.aws.impl;


import sunstone.aws.annotation.AwsEc2Instance;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.inject.Hostname;
import sunstone.core.CreaperUtils;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;

public class AwsIdentifiableSunstoneResourceUtils {
    static Hostname resolveHostname(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case EC2_INSTANCE:
                Instance ec2 = identification.get(store, Instance.class);
                return ec2::publicIpAddress;
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }

    static OnlineManagementClient resolveOnlineManagementClient(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == AwsIdentifiableSunstoneResource.EC2_INSTANCE) {
                AwsEc2Instance annotation = (AwsEc2Instance) identification.identification;
                if (annotation.mode() == OperatingMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(resolveHostname(identification, store).get(), annotation.standalone());
                } else if (annotation.mode() == OperatingMode.DOMAIN) {
                    return CreaperUtils.createDomainManagementClient(resolveHostname(identification, store).get(), annotation.domain());
                } else {
                    throw new UnsupportedSunstoneOperationException("Unknown operating mode specified for injecting OnlineManagementClient.");
                }
            } else {
                throw new UnsupportedSunstoneOperationException("Only AWS EC2 instance is supported for injecting OnlineManagementClient.");
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
    }
}
