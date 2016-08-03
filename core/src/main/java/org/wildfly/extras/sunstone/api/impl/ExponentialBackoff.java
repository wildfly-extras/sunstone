package org.wildfly.extras.sunstone.api.impl;

import org.slf4j.Logger;

/**
 * Provides an exponentially increasing delay ({@code Thread.sleep}) from the provided initial value
 * up to 10 seconds. Say the initial delay was 100 millis, then the 1st call to {@code delay} will wait
 * 100 millis, the 2nd call will wait 200 millis, then 400 millis, then 800 millis, then 1.6 seconds,
 * then 3.2 seconds, then 6.4 seconds, and each call since then will wait 10 seconds which is the maximum.
 */
final class ExponentialBackoff {
    private static final Logger LOGGER = SunstoneCoreLogger.SSH;

    // TODO configurable?
    private static final int MAX_DELAY = 10 * 1000;

    private int delay;

    ExponentialBackoff(int initialDelayInMillis) {
        this.delay = initialDelayInMillis;
    }

    void delay() throws InterruptedException {
        LOGGER.trace("Waiting {} millis", delay);

        Thread.sleep(delay);

        delay = Math.min(MAX_DELAY, 2 * delay);
    }
}
