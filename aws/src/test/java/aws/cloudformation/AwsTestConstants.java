package aws.cloudformation;


import sunstone.aws.impl.AwsConfig;

public class AwsTestConstants {
    public static final String TAG = "${sunstone.test.keypair.tag}";
    public static final String NAME_1 = "${sunstone.test.keypair.name1}";
    public static final String NAME_2 = "${sunstone.test.keypair.name2}";
    public static final String instanceName = "${sunstone.test.instance.name}";
    public static final String region = "${"+ AwsConfig.REGION + "}";
    public static final String region2 = "${sunstone.aws.region2}";
    public static final String mgmtUser = "${non.existing:admin}";
    public static final String mgmtPassword = "${non.existing:pass.1234}";
    public static final String mgmtPort = "${non.existing:9990}";
    public static final String mgmtHost = "${non.existing:master}";
    public static final String mgmtProfile = "${non.existing:default}";
}
