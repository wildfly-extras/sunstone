package org.wildfly.extras.sunstone.api.impl;

import com.google.inject.AbstractModule;
import org.jclouds.ssh.config.ConfiguresSshClient;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

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
        String sshImpl = System.getProperty("clouds.ssh");

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
