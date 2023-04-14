package aws.cloudformation;


public class AwsTestConstants {
    public static final String TAG = "sunstoneKeyTag";
    public static final String NAME_1 = "sunstoneKeyName1";
    public static final String NAME_2 = "sunstoneKeyName2";
    public static final String instanceName = "${non.existing:eapSunstoneInjectInstance}";
    // must be same as in MP Config
    public static final String region = "${ec2.region:us-east-1}";
    public static final String mgmtUser = "${non.existing:admin}";
    public static final String mgmtPassword = "${non.existing:pass.1234}";
    public static final String mgmtPort = "${non.existing:9990}";
    public static final String mgmtHost = "${non.existing:master}";
    public static final String mgmtProfile = "${non.existing:default}";
}
