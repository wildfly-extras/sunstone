package azure.armTemplates;


public class AzureTestConstants {
    public static final String VNET_TAG = "sunstoneVnet";
    public static final String VNET_NAME_1 = "sunstoneVnet1";

    public static final String VNET_NAME_2 = "sunstoneVnet2";

    public static final String IMAGE_REF = "${image.ref:/subscriptions/7dee6f21-9f05-414e-99fa-08d3215fb420/resourceGroups/istraka-test/providers/Microsoft.Compute/images/eap-test-image}";
    public static final String instanceName = "${non.existing:eapSunstoneInjectInstance}";

    public static final String eapMngmtUser = "${non.existing:admin}";
    public static final String eapMngmtPassword = "${non.existing:pass.1234}";
    public static final String eapMngmtPort = "${non.existing:9990}";
}
