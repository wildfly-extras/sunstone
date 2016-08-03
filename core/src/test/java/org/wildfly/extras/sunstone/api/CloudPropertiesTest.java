package org.wildfly.extras.sunstone.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.wildfly.extras.sunstone.api.impl.Config;

/**
 * Tests features of {@link CloudProperties} class.
 *
 */
public class CloudPropertiesTest {

    @Test
    public void testGetPropertiesPathForClass() {
        assertEquals("/org/wildfly/extras/sunstone/api/impl/Config.properties", CloudProperties.getPropertiesPathForClass(Config.class));
        assertEquals("/org/wildfly/extras/sunstone/api/impl/Config.CloudProvider.EC2.properties", CloudProperties.getPropertiesPathForClass(Config.CloudProvider.EC2.class));
    }
}
