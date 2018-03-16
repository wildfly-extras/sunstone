package org.wildfly.extras.sunstone.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wildfly.extras.sunstone.api.CloudProperties;

/**
 * Tests features of {@link ObjectProperties} class (loading properties, replacement, value substitutions).
 *
 */
public class ObjectPropertiesTest {

    /**
     * Tests most common use-cases - loading properties from classpath and resetting the {@link CloudProperties}.
     */
    @Test
    public void testPropertyLoadingClasspath() {
        final CloudProperties cloudProperties = CloudProperties.getInstance();
        // check if the fluent API returns the same instance
        assertEquals(cloudProperties, cloudProperties.reset().load(getClass()));

        ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testprovider");
        assertEquals("http://127.0.0.1:2375/", objectProperties.getProperty(Config.CloudProvider.Docker.ENDPOINT));
        // non existing property - getProperty returns null
        assertNull(objectProperties.getProperty("some.unknown.property"));
        // defaulting for non-existing property
        assertEquals("test", objectProperties.getProperty("some.unknown.property", "test"));

        // the prefixed properties names should not occur in objectProperties
        assertNull(objectProperties.getProperty("cloud.provider.testprovider.docker.endpoint"));

        // test to-int conversion
        assertEquals(112567, objectProperties.getPropertyAsInt("just.a.number", 0));
        // test defaulting for property-as-int
        assertEquals(12, objectProperties.getPropertyAsInt("just.a.number.nonexisting", 12));

        // test clean-up method for cloud properties
        cloudProperties.reset();
        objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testprovider");
        assertNull(objectProperties.getProperty(Config.CloudProvider.Docker.ENDPOINT));
    }

    /**
     * Test if {@value Config#DEFAULT_PROPERTIES} file is loaded correctly.
     */
    @Test
    public void testPropertyLoadingFromDefaultProperties() {
        final CloudProperties cloudProperties = CloudProperties.getInstance();

        cloudProperties.reset();

        // the content of the default properties file should be available after the reset() call
        ObjectProperties objectProperties = new ObjectProperties(ObjectType.NODE, "medium-instance");
        assertEquals("1024", objectProperties.getProperty(Config.Node.Docker.CPU_SHARES));

        cloudProperties.load(getClass());

        objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "docker");
        // the testtemplatedprovider doesn't have specified type, but its template has it. The "docker" template comes
        // from the default properties file
        assertEquals("docker", objectProperties.getProperty("type"));
        ObjectProperties nodeProperties = new ObjectProperties(ObjectType.NODE, "medium-instance");
        assertEquals("1024", nodeProperties.getProperty(Config.Node.Docker.CPU_SHARES));
        assertEquals(2048, nodeProperties.getPropertyAsInt(Config.Node.Docker.MEMORY_IN_MB, 0));
    }

    /**
     * Test if lookup for property value correctly fall-backs to template objects.
     */
    @Test
    public void testPropertyLoadingFromTemplate() {
        final CloudProperties cloudProperties = CloudProperties.getInstance();
        // check if the fluent API returns the same instance
        assertEquals(cloudProperties, cloudProperties.reset().load(getClass()));

        ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testtemplatedprovider");
        // testtemplatedprovider has defined template with name docker which is placed in default properties
        assertEquals("docker", objectProperties.getProperty("template"));
        // the testtemplatedprovider doesn't have specified type, but its template has it
        assertEquals("docker", objectProperties.getProperty("type"));

        // test more levels of templating
        ObjectProperties props2 = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testtemplatedprovider3");
        assertEquals("http://192.168.1.1:2375/", props2.getProperty("docker.endpoint"));
    }

    /**
     * Test if also files on system path (i.e. not only classpath) can be loaded.
     *
     * @throws IOException
     */
    @Test
    public void testPropertyLoadingSystempath() throws IOException {
        File tempFile = File.createTempFile("test-", ".objectproperties");
        try {
            FileUtils.write(tempFile, "cloud.provider.testprovider.docker.endpoint=http://my-server.example:2375/");

            CloudProperties.getInstance().reset().load(getClass())
                    .load(tempFile.getAbsolutePath());

            ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testprovider");
            assertEquals("docker", objectProperties.getProperty("type"));
            assertEquals("http://my-server.example:2375/", objectProperties.getProperty("docker.endpoint"));
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    /**
     * Tests if loaded object properties can be overriden by system properties.
     */
    @Test
    public void testSystemPropertiesOverride() {
        String propertyName = "cloud.provider.testprovider.docker.endpoint";
        try {
            System.setProperty(propertyName, "xxx");
            CloudProperties.getInstance().reset().load(getClass());

            ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testprovider");
            assertEquals("docker", objectProperties.getProperty("type"));
            assertEquals("xxx", objectProperties.getProperty("docker.endpoint"));
        } finally {
            System.clearProperty(propertyName);
        }
    }

    /**
     * Tests if single object properties can be additionally loaded from property file (where the values are stored without
     * suffix).
     *
     * @throws IOException
     */
    @Test
    public void testSingleObjectOverrides() throws IOException {
        File tempFile = File.createTempFile("test-", ".objectproperties");
        String propertyName = "cloud.provider.testprovider.properties";
        try {
            System.setProperty(propertyName, tempFile.getAbsolutePath());
            // write property without prefix (i.e. "type.name.")
            FileUtils.write(tempFile, "docker.endpoint=just-a-value");
            CloudProperties.getInstance().reset().load(getClass());

            ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, "testprovider");
            assertEquals("docker", objectProperties.getProperty("type"));
            assertEquals("just-a-value", objectProperties.getProperty("docker.endpoint"));
        } finally {
            System.clearProperty(propertyName);
            FileUtils.deleteQuietly(tempFile);
        }
    }

    /**
     * Tests property values substitutions.
     */
    @Test
    public void testPropertyReplacements() {
        testPropertyReplacementInternal("defaultdelimiter");
    }

    /**
     * Tests property values substitutions.
     */
    @Test
    public void testPropertyReplacementsCustomDelimiter() {
        try {
            System.setProperty(ObjectProperties.SYSTEM_PROPERTY_VALUE_DELIMITER_SST, ":-");
            testPropertyReplacementInternal("customdelimiter");
        } finally {
            System.clearProperty(ObjectProperties.SYSTEM_PROPERTY_VALUE_DELIMITER_SST);
        }
    }

    private void testPropertyReplacementInternal(final String objectName) {
        // cloud.provider.testprovider.replace1=${test.property}
        // cloud.provider.testprovider.replace2=${test.property:default value}
        // cloud.provider.testprovider.replace3=${test.property:112567}

        CloudProperties.getInstance().reset().load(getClass());
        try {
            ObjectProperties objectProperties = new ObjectProperties(ObjectType.CLOUD_PROVIDER, objectName);

            System.clearProperty("test.property");
            assertEquals("${test.property}", objectProperties.getProperty("replace1"));
            assertEquals("default value", objectProperties.getProperty("replace2"));
            assertEquals(112567, objectProperties.getPropertyAsInt("replace3", 0));

            System.setProperty("test.property", "abc");
            assertEquals("abc", objectProperties.getProperty("replace1"));
            assertEquals("abc", objectProperties.getProperty("replace2"));
            assertEquals(12, objectProperties.getPropertyAsInt("replace3", 12));

            System.setProperty("test.property", "abc ${test2.property}");
            System.clearProperty("test2.property");
            assertEquals("abc ${test2.property}", objectProperties.getProperty("replace1"));
            System.setProperty("test2.property", "xxx");
            assertEquals("abc xxx", objectProperties.getProperty("replace1"));
        } finally {
            System.clearProperty("test.property");
            System.clearProperty("test2.property");
        }
    }


}
