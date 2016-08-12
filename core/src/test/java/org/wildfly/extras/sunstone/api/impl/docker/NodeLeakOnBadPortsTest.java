package org.wildfly.extras.sunstone.api.impl.docker;

import static org.junit.Assert.assertTrue;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.PortOpeningException;

/**
 * Checks if node is correctly closed when the expected "start-up port" is not opened in given timeout.
 */
public class NodeLeakOnBadPortsTest {

    @Test
    public void test() {
        CloudProperties.getInstance().reset().load(this.getClass());
        try (DockerCloudProvider cloudProvider = (DockerCloudProvider) CloudProvider.create("provider0")) {
            final ComputeService computeService = cloudProvider.getComputeServiceContext().getComputeService();
            assertTrue("No node-group should be 'node-leak-test' before test", computeService.listNodes().stream()
                    .noneMatch(cm -> "node-leak-test".equals(((NodeMetadata) cm).getGroup())));
            try {
                cloudProvider.createNode("node0").close();
                Assert.fail("Port 2468 was unexpectedly opened");
            } catch (PortOpeningException e) {
                assertTrue("No node-group should be 'node-leak-test' after test", computeService.listNodes().stream()
                        .noneMatch(cm -> "node-leak-test".equals(((NodeMetadata) cm).getGroup())));
            }
        }
    }

}
