package sunstone.azure.armTemplates;


import sunstone.azure.impl.AzureConfig;

public class AzureTestConstants {
    public static final String VNET_TAG = "sunstoneVnet";
    public static final String VNET_NAME_1 = "sunstoneVnet1";

    public static final String VNET_NAME_2 = "sunstoneVnet2";

    public static final String IMAGE_REF = "${image.ref:/subscriptions/7dee6f21-9f05-414e-99fa-08d3215fb420/resourceGroups/istraka-test/providers/Microsoft.Compute/images/eap-test-image}";
    public static final String instanceName = "${sunstone.test.instance.name}";

    public static final String deployGroup = "${"+ AzureConfig.GROUP + "}";
    public static final String mgmtUser = "${non.existing:admin}";
    public static final String mgmtPassword = "${non.existing:pass.1234}";
    public static final String mgmtPort = "${non.existing:9990}";
    public static final String mgmtHost = "${non.existing:master}";
    public static final String mgmtProfile = "${non.existing:default}";
}
