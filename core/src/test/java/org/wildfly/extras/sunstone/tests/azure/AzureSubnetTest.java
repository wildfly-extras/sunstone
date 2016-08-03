package org.wildfly.extras.sunstone.tests.azure;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.Node;

import java.io.IOException;

import static org.junit.Assume.assumeFalse;

/**
 * Asserts that two nodes join a single common subnet and can ping each other on their private interface.
 * The name of the existing Azure virtual network must be in a system property called {@code azure.virtualNetwork},
 * the name of the existing subnet in the virtual network must be in a system property called {@code azure.subnet}.
 */
public class AzureSubnetTest {
    @BeforeClass
    public static void beforeClass() {
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.subscriptionId")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.privateKeyFile")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.privateKeyPassword")));

        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.virtualNetwork")));
        assumeFalse(Strings.isNullOrEmpty(System.getProperty("azure.subnet")));
    }

    @Before
    public final void setUp() throws IOException {
        CloudProperties.getInstance().reset().load(this.getClass());
    }

    @Test
    public void twoNodesOnASingleSubnetPing() throws IOException, InterruptedException {
        try (CloudProvider cloudProvider = CloudProvider.create("myprovider")) {
            try (Node node1 = cloudProvider.createNode("node1")) {
                try (Node node2 = cloudProvider.createNode("node2")) {
                    String node1PrivateAddress = node1.getPrivateAddress();
                    String node2PrivateAddress = node2.getPrivateAddress();

                    // send 4 ping probes, wait up to 20 seconds to receive responses
                    node1.exec("ping", "-c", "4", "-w", "20", node2PrivateAddress).assertSuccess();
                    node2.exec("ping", "-c", "4", "-w", "20", node1PrivateAddress).assertSuccess();
                }
            }
        }
    }
}
