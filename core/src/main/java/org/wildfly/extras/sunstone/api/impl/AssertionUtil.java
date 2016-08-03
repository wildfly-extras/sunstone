package org.wildfly.extras.sunstone.api.impl;

import com.google.common.base.Strings;

public final class AssertionUtil {
    private AssertionUtil() {} // avoid instantiation

    public static void fail(String messageFromUser, String defaultAssertMessage) {
        String message = Strings.isNullOrEmpty(messageFromUser)
                ? defaultAssertMessage
                : messageFromUser + "; " + defaultAssertMessage;
        throw new AssertionError(message);
    }
}
