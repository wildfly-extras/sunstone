package org.wildfly.extras.sunstone.api.impl.baremetal;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.wildfly.extras.sunstone.api.CloudProviderType;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsCloudProvider;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.DynamicSshClientModule;
import org.wildfly.extras.sunstone.api.impl.NodeGroupUtil;
import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectType;
import org.wildfly.extras.sunstone.api.impl.SocketFinderOnlyPublicInterfacesModule;
import org.wildfly.extras.sunstone.api.jclouds.JCloudsNode;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Booleans;

/**
 * Bare metal implementation of CloudProvider. This implementation uses JClouds internally.
 */
public class BareMetalCloudProvider extends AbstractJCloudsCloudProvider {
    public BareMetalCloudProvider(String name, Map<String, String> overrides) {
        super(name, CloudProviderType.BARE_METAL, overrides, BareMetalCloudProvider::createContextBuilder);
    }

    private static ContextBuilder createContextBuilder(ObjectProperties objectProperties) {
        Properties contextProperties = new Properties();

        String allNodeNames = Objects.requireNonNull(objectProperties.getProperty(Config.CloudProvider.BareMetal.NODES),
                "The Bare Metal provider requires that all nodes are identified upfront, using the "
                        + Config.CloudProvider.BareMetal.NODES + " property");

        Iterable<String> nodeNames = Splitter
                .on(",")
                .omitEmptyStrings()
                .trimResults()
                .split(allNodeNames);

        StringBuilder nodesYaml = new StringBuilder();
        nodesYaml.append("nodes:\n");
        for (String node : nodeNames) {
            ObjectProperties nodeProperties = new ObjectProperties(ObjectType.NODE, node);

            String host = Objects.requireNonNull(nodeProperties.getProperty(Config.Node.BareMetal.HOST),
                    "Host name or IP address must be set for node '" + node + "'");
            String sshUser = Objects.requireNonNull(nodeProperties.getProperty(Config.Node.BareMetal.SSH_USER),
                    "SSH user name must be set for node '" + node + "'");
            String sshPassword = nodeProperties.getProperty(Config.Node.BareMetal.SSH_PASSWORD);
            String sshPrivateKey = nodeProperties.getProperty(Config.Node.BareMetal.SSH_PRIVATE_KEY);
            Path sshPrivateKeyFile = nodeProperties.getPropertyAsPath(Config.Node.BareMetal.SSH_PRIVATE_KEY_FILE, null);
            if (Booleans.countTrue(sshPassword != null, sshPrivateKey != null, sshPrivateKeyFile != null) != 1) {
                throw new IllegalArgumentException("Exactly one of SSH password or private key or private key file must be set for node '" + node + "'");
            }

            String nodeGroup = NodeGroupUtil.nodeGroupName(nodeProperties, objectProperties);

            nodesYaml.append("    - id: ").append(node).append("\n");
            nodesYaml.append("      name: ").append(node).append("\n");
            nodesYaml.append("      hostname: ").append(host).append("\n");
            nodesYaml.append("      os_family: immaterial\n"); // required by JClouds, but immaterial
            nodesYaml.append("      os_description: immaterial\n"); // required by JClouds, but immaterial
            nodesYaml.append("      group: ").append(nodeGroup).append("\n");
            nodesYaml.append("      username: ").append(sshUser).append("\n");
            if (sshPassword != null) {
                nodesYaml.append("      credential: ").append(sshPassword).append("\n");
            } else if (sshPrivateKey != null) {
                nodesYaml.append("      credential: ").append(sshPrivateKey).append("\n");
            } else {
                nodesYaml.append("      credential_url: file://").append(sshPrivateKeyFile).append("\n");
            }
        }

        contextProperties.setProperty("byon.nodes", nodesYaml.toString());

        return ContextBuilder.newBuilder("byon")
                .overrides(contextProperties)
                .modules(ImmutableSet.of(
                        new SLF4JLoggingModule(),
                        new DynamicSshClientModule(),
                        new SocketFinderOnlyPublicInterfacesModule()
                ));
    }

    @Override
    protected JCloudsNode createNodeInternal(String name, Map<String, String> overrides) {
        return new BareMetalNode(this, name, overrides);
    }

    @Override
    public boolean nodeRequiresDestroy() {
        return false;
    }
}
