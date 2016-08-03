package org.wildfly.extras.sunstone.api.impl;

import org.jclouds.compute.domain.ExecChannel;
import org.wildfly.extras.sunstone.api.ssh.CommandExecution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JCloudsCommandExecution implements CommandExecution {
    private final ExecChannel jclouds;

    public JCloudsCommandExecution(ExecChannel jclouds) {
        this.jclouds = jclouds;
    }

    @Override
    public OutputStream stdin() {
        return jclouds.getInput();
    }

    @Override
    public InputStream stdout() {
        return jclouds.getOutput();
    }

    @Override
    public InputStream stderr() {
        return jclouds.getError();
    }

    @Override
    public void await() throws InterruptedException {
        ExponentialBackoff backoff = new ExponentialBackoff(100);
        while (true) {
            Integer exitStatus = jclouds.getExitStatus().get();
            if (exitStatus != null) {
                return;
            }

            backoff.delay();
        }
    }

    @Override
    public void await(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        ExponentialBackoff backoff = new ExponentialBackoff(100);
        long endTime = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime) {
            Integer exitStatus = jclouds.getExitStatus().get();
            if (exitStatus != null) {
                return;
            }

            backoff.delay();
        }

        if (jclouds.getExitStatus().get() == null) {
            throw new TimeoutException();
        }
    }

    @Override
    public OptionalInt exitCode() {
        Integer exitStatus = jclouds.getExitStatus().get();
        return exitStatus != null ? OptionalInt.of(exitStatus) : OptionalInt.empty();
    }

    @Override
    public void close() throws IOException {
        jclouds.close();
    }
}
