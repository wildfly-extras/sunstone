package sunstone.aws.impl;


import sunstone.annotation.WildFly;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import software.amazon.awssdk.services.ec2.model.Instance;
import sunstone.annotation.OperatingMode;
import sunstone.inject.Hostname;
import sunstone.core.CreaperUtils;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;

public class AwsWFIdentifiableSunstoneResourceUtils {
    static Hostname resolveHostname(AwsWFIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case EC2_INSTANCE:
                Instance ec2 = identification.get(store, Instance.class);
                return ec2::publicIpAddress;
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }

    static OnlineManagementClient resolveOnlineManagementClient(AwsWFIdentifiableSunstoneResource.Identification identification, WildFly wildfly, AwsSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == AwsWFIdentifiableSunstoneResource.EC2_INSTANCE) {
                if (wildfly.mode() == OperatingMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(AwsWFIdentifiableSunstoneResourceUtils.resolveHostname(identification, store).get(), wildfly.standalone());
                } else if (wildfly.mode() == OperatingMode.DOMAIN) {
                    return CreaperUtils.createDomainManagementClient(resolveHostname(identification, store).get(), wildfly.domain());
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
