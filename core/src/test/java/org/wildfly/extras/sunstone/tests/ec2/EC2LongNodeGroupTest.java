package org.wildfly.extras.sunstone.tests.ec2;

import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.ec2.EC2CloudProvider;
import org.wildfly.extras.sunstone.api.impl.ec2.EC2Node;

import static org.junit.Assume.assumeFalse;

/**
 * The test tries to create an EC2 instance with a very long name. Since EC2 instances are limited
 * to 63 characters and this name is more than 100 characters long, the test will fail, unless
 * name postprocessing properly shortens the instance name.
 *
 * The test requires {@code ec2.accessKeyID} and {@code ec2.secretAccessKey} properties to be set.
 */
public class EC2LongNodeGroupTest {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    @BeforeClass
    public static void setUpClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.accessKeyID")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("ec2.secretAccessKey")));
    }

    @Test
    public void testLongNodeGroup() {
        CloudProperties.getInstance().reset().load(this.getClass());
        try (EC2CloudProvider ec2CloudProvider = (EC2CloudProvider) CloudProvider.create("myprovider")) {
            EC2Node ec2Node = (EC2Node) ec2CloudProvider.createNode("mynode");
            ec2Node.close();
        } catch (Exception e) {
            LOGGER.error("An error occurred in EC2LongNodeGroupTest: ", e);
            Assert.fail("No exceptions are expected when starting a node with long node group, but we got: " + e);
        }
    }
}
