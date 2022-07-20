package org.jboss.eapqe.clouds.ec2;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import sunstone.api.Sunstone;
import sunstone.api.WithAwsCfTemplate;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import sunstone.core.AwsClientFactory;

//@Sunstone
//@WithAwsCfTemplate("/messaging-ha.yaml")
public class MessagingHATest {

    @Test
    public void test() {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder().build();
        AwsCredentials awsCredentials = credentialsProvider.resolveCredentials();


//    }
//    public static void createCFStack(CloudFormationClient cfClient,
//                                     String stackName,
//                                     String roleARN,
//                                     String location,
//                                     String key,
//                                     String value){




        try {
            CloudFormationClient cfClient = AwsClientFactory.getCloudFormationClient();

            CloudFormationWaiter waiter = cfClient.waiter();
//            Parameter myParameter = Parameter.builder()
//                    .parameterKey(key)
//                    .parameterValue(value)
//                    .build();

            CreateStackRequest stackRequest = CreateStackRequest.builder()
                    .stackName("istraka-stack")
                    .templateBody(template)//templateURL(location)
                    .onFailure(OnFailure.ROLLBACK)
                    .build();

            cfClient.createStack(stackRequest);
            DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                    .stackName("istraka-stack")
                    .build();

            WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackCreateComplete(stacksRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println("istraka-stack" +" is ready");

        } catch (CloudFormationException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
    static String template = "AWSTemplateFormatVersion: \"2010-09-09\"\n"
            + "Metadata:\n"
            + "    Generator: \"former2\"\n"
            + "Description: \"\"\n"
            + "Resources:\n"
            + "    EC2VPC:\n"
            + "        Type: \"AWS::EC2::VPC\"\n"
            + "        Properties:\n"
            + "            CidrBlock: \"172.31.0.0/16\"\n"
            + "            EnableDnsSupport: true\n"
            + "            EnableDnsHostnames: true\n"
            + "            InstanceTenancy: \"default\"\n"
            + "\n"
            + "    EFSFileSystem:\n"
            + "        Type: \"AWS::EFS::FileSystem\"\n"
            + "        Properties:\n"
            + "            PerformanceMode: \"generalPurpose\"\n"
            + "            Encrypted: false\n"
            + "            ThroughputMode: \"bursting\"\n"
            + "\n"
            + "    EFSMountTarget:\n"
            + "        Type: \"AWS::EFS::MountTarget\"\n"
            + "        Properties:\n"
            + "            FileSystemId: !Ref EFSFileSystem\n"
            + "            IpAddress: \"172.31.15.87\"\n"
            + "            SecurityGroups:\n"
            + "              - !Ref EC2SecurityGroup\n"
            + "            SubnetId: !Ref EC2Subnet\n"
            + "\n"
            + "    EC2SecurityGroup:\n"
            + "        Type: \"AWS::EC2::SecurityGroup\"\n"
            + "        Properties:\n"
            + "            GroupDescription: \"Allows any connection\"\n"
            + "            GroupName: \"allow-any\"\n"
            + "            VpcId: !Ref EC2VPC\n"
            + "            SecurityGroupIngress:\n"
            + "              -\n"
            + "                CidrIp: \"0.0.0.0/0\"\n"
            + "                IpProtocol: \"-1\"\n"
            + "              -\n"
            + "                CidrIpv6: \"::/0\"\n"
            + "                IpProtocol: \"-1\"\n"
            + "            SecurityGroupEgress:\n"
            + "              -\n"
            + "                CidrIp: \"0.0.0.0/0\"\n"
            + "                IpProtocol: \"-1\"\n"
            + "\n"
            + "    EC2Instance:\n"
            + "        Type: \"AWS::EC2::Instance\"\n"
            + "        Properties:\n"
            + "            ImageId: \"ami-00753c9d2b9d4273c\"\n"
            + "            InstanceType: \"t3.medium\"\n"
            + "            KeyName: \"jclouds-jenkins\"\n"
            + "            AvailabilityZone: !GetAtt EC2Subnet.AvailabilityZone\n"
            + "            Tenancy: \"default\"\n"
            + "            SubnetId: !Ref EC2Subnet\n"
            + "            EbsOptimized: false\n"
            + "            SecurityGroupIds:\n"
            + "              - !Ref EC2SecurityGroup\n"
            + "            SourceDestCheck: true\n"
            + "            BlockDeviceMappings:\n"
            + "              -\n"
            + "                DeviceName: \"/dev/sda1\"\n"
            + "                Ebs:\n"
            + "                    Encrypted: false\n"
            + "                    VolumeSize: 10\n"
            + "                    VolumeType: \"gp2\"\n"
            + "                    DeleteOnTermination: true\n"
            + "            Tags:\n"
            + "              -\n"
            + "                Key: \"Name\"\n"
            + "                Value: \"eapqe-ha-live\"\n"
            + "            HibernationOptions:\n"
            + "                Configured: false\n"
            + "            CpuOptions:\n"
            + "                CoreCount: 1\n"
            + "                ThreadsPerCore: 2\n"
            + "            EnclaveOptions:\n"
            + "                Enabled: false\n"
            + "\n"
            + "    EC2Instance2:\n"
            + "        Type: \"AWS::EC2::Instance\"\n"
            + "        Properties:\n"
            + "            ImageId: \"ami-00753c9d2b9d4273c\"\n"
            + "            InstanceType: \"t3.medium\"\n"
            + "            KeyName: \"jclouds-jenkins\"\n"
            + "            AvailabilityZone: !GetAtt EC2Subnet.AvailabilityZone\n"
            + "            Tenancy: \"default\"\n"
            + "            SubnetId: !Ref EC2Subnet\n"
            + "            EbsOptimized: false\n"
            + "            SecurityGroupIds:\n"
            + "              - !Ref EC2SecurityGroup\n"
            + "            SourceDestCheck: true\n"
            + "            BlockDeviceMappings:\n"
            + "              -\n"
            + "                DeviceName: \"/dev/sda1\"\n"
            + "                Ebs:\n"
            + "                    Encrypted: false\n"
            + "                    VolumeSize: 10\n"
            + "                    VolumeType: \"gp2\"\n"
            + "                    DeleteOnTermination: true\n"
            + "            Tags:\n"
            + "              -\n"
            + "                Key: \"Name\"\n"
            + "                Value: \"eapqe-ha-client\"\n"
            + "            HibernationOptions:\n"
            + "                Configured: false\n"
            + "            CpuOptions:\n"
            + "                CoreCount: 1\n"
            + "                ThreadsPerCore: 2\n"
            + "            EnclaveOptions:\n"
            + "                Enabled: false\n"
            + "\n"
            + "    EC2Instance3:\n"
            + "        Type: \"AWS::EC2::Instance\"\n"
            + "        Properties:\n"
            + "            ImageId: \"ami-00753c9d2b9d4273c\"\n"
            + "            InstanceType: \"t3.medium\"\n"
            + "            KeyName: \"jclouds-jenkins\"\n"
            + "            AvailabilityZone: !GetAtt EC2Subnet.AvailabilityZone\n"
            + "            Tenancy: \"default\"\n"
            + "            SubnetId: !Ref EC2Subnet\n"
            + "            EbsOptimized: false\n"
            + "            SecurityGroupIds:\n"
            + "              - !Ref EC2SecurityGroup\n"
            + "            SourceDestCheck: true\n"
            + "            BlockDeviceMappings:\n"
            + "              -\n"
            + "                DeviceName: \"/dev/sda1\"\n"
            + "                Ebs:\n"
            + "                    Encrypted: false\n"
            + "                    VolumeSize: 10\n"
            + "                    VolumeType: \"gp2\"\n"
            + "                    DeleteOnTermination: true\n"
            + "            Tags:\n"
            + "              -\n"
            + "                Key: \"Name\"\n"
            + "                Value: \"eapqe-ha-backup\"\n"
            + "            HibernationOptions:\n"
            + "                Configured: false\n"
            + "            CpuOptions:\n"
            + "                CoreCount: 1\n"
            + "                ThreadsPerCore: 2\n"
            + "            EnclaveOptions:\n"
            + "                Enabled: false\n"
            + "\n"
            + "    EC2NetworkInterface2:\n"
            + "        Type: \"AWS::EC2::NetworkInterface\"\n"
            + "        Properties:\n"
            + "            Description: !Sub \"EFS mount target for ${EFSFileSystem} (${EFSMountTarget})\"\n"
            + "            SubnetId: !Ref EC2Subnet\n"
            + "            SourceDestCheck: true\n"
            + "            GroupSet:\n"
            + "              - !Ref EC2SecurityGroup\n"
            + "\n"
            + "    EC2Subnet:\n"
            + "        Type: \"AWS::EC2::Subnet\"\n"
            + "        DependsOn:\n"
            + "          - EC2VPC\n"
            + "        Properties:\n"
            + "            AvailabilityZone: !Sub \"${AWS::Region}a\"\n"
            + "            CidrBlock: \"172.31.0.0/20\"\n"
            + "            VpcId: !Ref EC2VPC\n"
            + "            MapPublicIpOnLaunch: true\n"
            + "\n"
            + "    EC2InternetGateway:\n"
            + "        Type: \"AWS::EC2::InternetGateway\"\n"
            + "\n"
            + "    EC2Route:\n"
            + "        Type: \"AWS::EC2::Route\"\n"
            + "        DependsOn:\n"
            + "          - EC2Subnet\n"
            + "          - EC2InternetGateway\n"
            + "          - EC2RouteTable\n"
            + "        Properties:\n"
            + "            DestinationCidrBlock: \"0.0.0.0/0\"\n"
            + "            GatewayId: !Ref EC2InternetGateway\n"
            + "            RouteTableId: !Ref EC2RouteTable\n"
            + "\n"
            + "    EC2VPCGatewayAttachment:\n"
            + "        Type: \"AWS::EC2::VPCGatewayAttachment\"\n"
            + "        Properties:\n"
            + "            InternetGatewayId: !Ref EC2InternetGateway\n"
            + "            VpcId: !Ref EC2VPC\n"
            + "\n"
            + "    EC2RouteTable:\n"
            + "        Type: \"AWS::EC2::RouteTable\"\n"
            + "        Properties:\n"
            + "            VpcId: !Ref EC2VPC\n"
            + "\n"
            + "    EC2SubnetRouteTableAssociation:\n"
            + "        DependsOn:\n"
            + "          - EC2Route\n"
            + "        Type: \"AWS::EC2::SubnetRouteTableAssociation\"\n"
            + "        Properties:\n"
            + "            RouteTableId: !Ref EC2RouteTable\n"
            + "            SubnetId: !Ref EC2Subnet\n";
}
