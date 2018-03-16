package org.wildfly.extras.sunstone.api.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import org.jclouds.ssh.config.ConfiguresSshClient;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;

@ConfiguresSshClient
public final class DynamicSshClientModule extends AbstractModule {
    private static final Logger LOG = SunstoneCoreLogger.SSH;

    private static AtomicBoolean alreadyLogged = new AtomicBoolean();

    private static void logOnce(String sshImplDescription) {
        if (alreadyLogged.compareAndSet(false, true)) {
            LOG.debug("Using " + sshImplDescription + " SSH client library");
        }
    }

    @Override
    protected void configure() {
        final String oldSshClientProperty = "clouds.ssh";
        final String sshClientProperty = "sunstone.ssh";
        String sshImpl = System.getProperty(sshClientProperty);
        if (Strings.isNullOrEmpty(sshImpl)) {
            sshImpl = System.getProperty(oldSshClientProperty);
            if (!Strings.isNullOrEmpty(sshImpl)) {
                LOG.warn("Property {} is deprecated, use {} instead", oldSshClientProperty, sshClientProperty);
            }
        }

        if ("sshj".equalsIgnoreCase(sshImpl)) {
            logOnce("SSHJ");
            install(new SshjSshClientModule());
        } else {
            // default
            logOnce("JSCH");
            install(new JschSshClientModule());
        }
    }
}
