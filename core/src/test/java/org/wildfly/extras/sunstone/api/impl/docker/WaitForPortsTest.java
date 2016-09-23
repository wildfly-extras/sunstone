package org.wildfly.extras.sunstone.api.impl.docker;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.Node;

/**
 * Checks if waiting for start ports before and after the boot script execution works.
 */
public class WaitForPortsTest {

    @Test
    public void test() {
        CloudProperties.getInstance().reset().load(this.getClass());
        try (CloudProvider cloudProvider = CloudProvider.create("provider0")) {
            try (Node node = cloudProvider.createNode("node0")) {
                assertTrue(node.isPortOpen(8080));
                assertTrue(node.isPortOpen(9990));
            }
        }
    }

}
