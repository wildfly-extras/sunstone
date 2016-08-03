package org.wildfly.extras.sunstone.api.impl.baremetal;

import com.google.common.collect.Iterables;
import org.jclouds.compute.domain.NodeMetadata;
import org.slf4j.Logger;
import org.wildfly.extras.sunstone.api.OperationNotSupportedException;
import org.wildfly.extras.sunstone.api.impl.AbstractJCloudsNode;
import org.wildfly.extras.sunstone.api.impl.SunstoneCoreLogger;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.NodeConfigData;

import java.util.Map;

/**
 * Bare metal implementation of {@link org.wildfly.extras.sunstone.api.Node}. This implementation uses JClouds internally.
 */
public class BareMetalNode extends AbstractJCloudsNode<BareMetalCloudProvider> {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private static final NodeConfigData BARE_METAL_NODE_CONFIG_DATA = new NodeConfigData(
            Config.Node.BareMetal.WAIT_FOR_PORTS,
            Config.Node.BareMetal.WAIT_FOR_PORTS_TIMEOUT_SEC,
            30
    );

    private final NodeMetadata initialNodeMetadata;

    public BareMetalNode(BareMetalCloudProvider cloudProvider, String name, Map<String, String> configOverrides) {
        super(cloudProvider, name, configOverrides, BARE_METAL_NODE_CONFIG_DATA);

        if (!computeService
                .listNodes()
                .stream()
                .filter(cm -> cm.getProviderId().equals(name))
                .findAny()
                .isPresent()) {
            throw new IllegalStateException("Node '" + name + "' not configured in the Bare Metal cloud provider '"
                    + cloudProvider.getName() + "', use " + Config.CloudProvider.BareMetal.NODES);
        }

        this.initialNodeMetadata = computeService.getNodeMetadata(name);
        String publicAddress = Iterables.getFirst(initialNodeMetadata.getPublicAddresses(), null);
        LOGGER.info("Obtained {} node '{}', its public IP address is {}",
                cloudProvider.getCloudProviderType().getHumanReadableName(), name, publicAddress);
        waitForStartPorts();
    }

    @Override
    public NodeMetadata getInitialNodeMetadata() {
        return initialNodeMetadata;
    }

    @Override
    public NodeMetadata getFreshNodeMetadata() {
        return computeService.getNodeMetadata(initialNodeMetadata.getId());
    }

    @Override
    public String getImageName() {
        throw new OperationNotSupportedException("The Bare Metal provider has no concept of images");
    }

    @Override
    public String getPrivateAddress() {
        // no such thing as private address, but we should return something
        // TODO this should probably be configurable
        return getPublicAddress();
    }

    @Override
    public void stop() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("The Bare Metal provider has no concept of node lifecycle");
    }

    @Override
    public void start() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("The Bare Metal provider has no concept of node lifecycle");
    }

    @Override
    public void kill() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("The Bare Metal provider has no concept of node lifecycle");
    }
}
