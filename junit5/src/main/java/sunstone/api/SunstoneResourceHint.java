package sunstone.api;


/**
 * Hint to Sunstone for various operations: inject - {@link SunstoneResource}, deploy archive to WildFly {@link  WildFlyDeployment}, ...
 */
public enum SunstoneResourceHint {

    NONE,
    JCLOUDS_NODE,
    AWS_EC2_INSTANCE,
    AZ_VM_INSTANCE,
    AZ_WEB_APP;
}
