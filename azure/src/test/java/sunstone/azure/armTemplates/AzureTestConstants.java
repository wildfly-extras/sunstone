package sunstone.azure.armTemplates;


import sunstone.azure.impl.AzureConfig;

public class AzureTestConstants {
    public static final String VNET_TAG = "sunstoneVnet";
    public static final String VNET_NAME_1 = "sunstoneVnet1";

    public static final String VNET_NAME_2 = "sunstoneVnet2";

    public static final String instanceName = "${sunstone.test.instance.name}";

    public static final String deployGroup = "${"+ AzureConfig.GROUP + "}";
    public static final String mgmtUser = "${non.existing:admin}";
    public static final String mgmtPassword = "${non.existing:pass.1234}";
    public static final String mgmtPort = "${non.existing:9990}";
    public static final String mgmtHost = "${non.existing:master}";
    public static final String mgmtProfile = "${non.existing:default}";
    public static final String IMAGE_MARKETPLACE_PLAN = "${ts.azure.eap.image.plan.name}";
    public static final String IMAGE_MARKETPLACE_PUBLISHER = "${ts.azure.eap.image.publisher}";
    public static final String IMAGE_MARKETPLACE_PRODUCT = "${ts.azure.eap.image.product}";
    public static final String IMAGE_MARKETPLACE_OFFER = "${ts.azure.eap.image.offer}";
    public static final String IMAGE_MARKETPLACE_SKU = "${ts.azure.eap.image.sku}";
    public static final String IMAGE_MARKETPLACE_VERSION = "${ts.azure.eap.image.version}";
}
