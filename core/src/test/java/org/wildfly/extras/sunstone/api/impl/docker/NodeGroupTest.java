package org.wildfly.extras.sunstone.api.impl.docker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.impl.Constants;

public class NodeGroupTest {

    @Test
    public void test() {
        CloudProperties.getInstance().reset().load(NodeGroupTest.class);

        try (CloudProvider cloudProvider = CloudProvider.create("provider0")) {
            try (DockerNode node = (DockerNode) cloudProvider.createNode("node0")) {
                assertEquals(Constants.JCLOUDS_NODEGROUP, node.getInitialNodeMetadata().getGroup());
            }

            try (DockerNode node = (DockerNode) cloudProvider.createNode("node1")) {
                assertEquals("my-node-group", node.getInitialNodeMetadata().getGroup());
            }
        }

        try (CloudProvider cloudProvider = CloudProvider.create("provider1")) {
            try (DockerNode node = (DockerNode) cloudProvider.createNode("node0")) {
                assertEquals("my-cloud-provider-group", node.getInitialNodeMetadata().getGroup());
            }

            try (DockerNode node = (DockerNode) cloudProvider.createNode("node1")) {
                assertEquals("my-node-group", node.getInitialNodeMetadata().getGroup());
            }
        }
    }
}
