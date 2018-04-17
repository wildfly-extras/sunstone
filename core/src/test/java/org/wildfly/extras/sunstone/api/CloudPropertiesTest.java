package org.wildfly.extras.sunstone.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.wildfly.extras.sunstone.api.impl.Config;
import org.wildfly.extras.sunstone.api.impl.ObjectType;

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

    @Test
    public void testTemplateTo() {
        CloudProperties cp = CloudProperties.getInstance().reset().load(CloudPropertiesTest.class);
        assertEquals("node2,node3,node4", cp.getConfigMap().get(templateToProp("node1")));
        assertEquals("node1", cp.getConfigMap().get(templateProp("node2")));
        assertEquals("node1", cp.getConfigMap().get(templateProp("node4")));
        assertEquals("node6", cp.getConfigMap().get(templateToProp("node5")));
        assertEquals("node5", cp.getConfigMap().get(templateProp("node6")));
    }

    @Test
    public void testTemplateToConflict() {
        String propertiesPath = CloudProperties.getPropertiesPathForClass(CloudPropertiesTest.class)
                .replace("CloudPropertiesTest", "CloudPropertiesTest-conflict");
        try {
            CloudProperties.getInstance().reset().load(propertiesPath);
            fail("Loading properties with conflicting template declarations (template vs. templateTo) should fail");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testGetListOfCopies() {
        CloudProperties.getInstance().reset().load(CloudPropertiesTest.class);
        Set<String> copies = CloudProperties.getSetOfCopies("node1");
        assertEquals(4, copies.size());

        Collection<String> expected = Arrays.asList("node2", "node3", "node4", "node7");
        assertTrue(String.format("Collection does not contain expected strings. Expected: %s, actual: %s"
                , String.join(",", expected), String.join(",", copies))
                , copies.containsAll(expected));
    }

    private String templateToProp(String node) {
        return ObjectType.NODE.getPropertyPrefix() + "." + node + "." + Config.TEMPLATE_TO;
    }

    private String templateProp(String node) {
        return ObjectType.NODE.getPropertyPrefix() + "." + node + "." + Config.TEMPLATE;
    }
}
