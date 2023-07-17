package sunstone.aws.impl;


import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.rds.model.DBInstance;
import sunstone.inject.Hostname;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;


public class AwsIdentifiableSunstoneResourceUtils {
    static Hostname resolveHostname(AwsIdentifiableSunstoneResource.Identification identification, AwsSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case EC2_INSTANCE:
                Instance ec2 = identification.get(store, Instance.class);
                return ec2::publicIpAddress;
            case RDS_SERVICE:
                DBInstance rds = identification.get(store, DBInstance.class);
                return () -> rds.endpoint().address();
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }
}
