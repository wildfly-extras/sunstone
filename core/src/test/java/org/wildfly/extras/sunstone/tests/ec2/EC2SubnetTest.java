package org.wildfly.extras.sunstone.tests.ec2;

import com.google.common.base.Strings;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.aws.ec2.domain.AWSRunningInstance;
import org.jclouds.ec2.domain.Reservation;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ec2.EC2CloudProvider;
import org.wildfly.extras.sunstone.api.impl.ec2.EC2Node;

import java.util.Set;

/**
 * This tests the subnet functionality for EC2. For test case documentation, see the corresponding
 * method.
 *
 * To run this test, {@code ec2.securityGroupIds} and {@code ec2.subnetId} system properties have
 * to be configured. That means, you'll need to have a subnet and a VPC security group already
 * created in your EC2 account. Note that this really requires security group IDs, not just names.
 */
public class EC2SubnetTest {

    @BeforeClass
    public static void setUpClass() {
        Assume.assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.securityGroupIds")));
        Assume.assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.subnetId")));
    }

    /**
     * Starts a node, then queries the AWS EC2 API to find out if the node really belongs to the
     * subnet that was specified in the .properties file.
     */
    @Test
    public void subnetTest() {
        CloudProperties.getInstance().reset().load(EC2SubnetTest.class);
        try (EC2CloudProvider ec2CloudProvider = (EC2CloudProvider) CloudProvider.create("provider0")) {
            EC2Node ec2Node = (EC2Node) ec2CloudProvider.createNode("node0");

            boolean nodeAmongChecked = false;
            String instanceId = ec2Node.getInitialNodeMetadata().getId().split("/")[1];
            String region = ec2Node.config().getProperty(Config.CloudProvider.EC2.REGION);
            String subnetId = ec2Node.config().getProperty(Config.Node.EC2.SUBNET_ID);
            Set<? extends Reservation<? extends AWSRunningInstance>> reservationsForRunningInstances =
                    ec2CloudProvider.getComputeServiceContext().unwrapApi(AWSEC2Api.class).getInstanceApi().get().describeInstancesInRegion(region);

            for (Reservation<? extends AWSRunningInstance> reservationForRunningInstances : reservationsForRunningInstances) {
                for (AWSRunningInstance runningInstance : reservationForRunningInstances) {
                    if (instanceId.equals(runningInstance.getId())) {
                        if (subnetId.equals(runningInstance.getSubnetId())) {
                            nodeAmongChecked = true;
                        } else {
                            Assert.fail("Created instance " + instanceId + " does not belong to subnet " + subnetId);
                        }
                    }
                }
            }

            Assert.assertTrue("The node we have created in this test is not present among the nodes we could check", nodeAmongChecked);
            ec2Node.close();
        }
    }
}
