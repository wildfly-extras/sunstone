package aws.cloudformation;


public class AwsTestConstants {
    public static final String instanceName = "${sunstone.test.instance.name}";
    public static final String region = "${sunstone.aws.region}";
    public static final String mgmtUser = "${non.existing:admin}";
    public static final String mgmtPassword = "${non.existing:pass.1234}";
    public static final String mgmtPort = "${non.existing:9990}";
    public static final String mgmtHost = "${non.existing:master}";
    public static final String mgmtProfile = "${non.existing:default}";
}
