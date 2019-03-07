package org.wildfly.extras.sunstone.api.impl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.jclouds.compute.config.ComputeServiceProperties;

public final class SocketFinderAllInterfacesModule extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant()
                .annotatedWith(Names.named(ComputeServiceProperties.SOCKET_FINDER_ALLOWED_INTERFACES))
                .to("ALL");
    }
}
