package org.wildfly.extras.sunstone.arquillian;

import org.wildfly.extras.sunstone.api.impl.ObjectProperties;
import org.wildfly.extras.sunstone.api.impl.ObjectPropertiesType;

/**
 * {@link ObjectProperties} Types used by our Arquillian extension.
 *
 */
public enum ArquillianObjectType implements ObjectPropertiesType {
    TESTSUITE("arquillian.suite", "Arquillian Suite");

    private final String propertyPrefix;
    private final String humanReadableName;

    ArquillianObjectType(String prefix, String humanReadableName) {
        this.propertyPrefix = prefix;
        this.humanReadableName = humanReadableName;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }
}
