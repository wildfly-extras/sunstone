package aws.cloudformation.di;


import sunstone.aws.api.AwsAutoResolve;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import sunstone.core.SunstoneExtension;

import static aws.cloudformation.AwsTestConstants.region;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SunstoneExtension.class)
public class AwsClientsTests {

    @AwsAutoResolve(region = region)
    static Ec2Client staticEC2ClientWithRegion;

    @AwsAutoResolve
    static Ec2Client staticEC2Client;

    @AwsAutoResolve(region = region)
    static S3Client staticS3ClientWithRegion;

    @AwsAutoResolve
    static S3Client staticS3Client;

    @AwsAutoResolve(region = region)
    Ec2Client ec2ClientWithRegion;

    @AwsAutoResolve
    Ec2Client ec2Client;

    @AwsAutoResolve(region = region)
    S3Client s3ClientWithRegion;

    @AwsAutoResolve
    S3Client s3Client;

    @BeforeAll
    public static void verifyStaticDI() {
        assertThat(staticS3Client).isNotNull();
        assertThat(staticS3ClientWithRegion).isNotNull();
        assertThat(staticEC2Client).isNotNull();
        assertThat(staticEC2ClientWithRegion).isNotNull();
    }

    @Test
    public void testClients() {
        assertThat(staticS3Client.listBuckets()).isNotNull();
        assertThat(staticS3ClientWithRegion.listBuckets()).isNotNull();
        assertThat(s3Client.listBuckets()).isNotNull();
        assertThat(s3ClientWithRegion.listBuckets()).isNotNull();
        assertThat(staticEC2Client.describeInstances()).isNotNull();
        assertThat(staticEC2ClientWithRegion.describeInstances()).isNotNull();
        assertThat(ec2Client.describeInstances()).isNotNull();
        assertThat(ec2ClientWithRegion.describeInstances()).isNotNull();
    }
}