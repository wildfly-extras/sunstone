package org.wildfly.extras.sunstone.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.sunstone.annotations.InjectNode;
import org.wildfly.extras.sunstone.annotations.WithNode;
import org.wildfly.extras.sunstone.annotations.WithWildFlyContainer;
import org.wildfly.extras.sunstone.api.Node;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * This class tests if the starting nodes works on class-level using {@link WithNode} and {@link WithWildFlyContainer} annotaions works.
 *
 */
@RunWith(Arquillian.class)
@WithNode("alpine-ssh")
@WithWildFlyContainer("node0")
public class ClassLevelNodeTest {

    @InjectNode("alpine-ssh")
    @ArquillianResource
    private Node alpineNode;

    @Deployment(name = "testClassNode", testable = false)
    @TargetsContainer("node0")
    public static WebArchive createDeploymentForClassLevel() {
        return ShrinkWrap.create(WebArchive.class, "testClassNode.war").addAsWebResource(new StringAsset("testClassNode"),
                "index.html");
    }

    @Deployment(name = "testSuiteNode", testable = false)
    @TargetsContainer("suite-level-2")
    public static WebArchive createDeploymentForSuiteLevel() {
        return ShrinkWrap.create(WebArchive.class, "testSuiteNode.war").addAsWebResource(new StringAsset("testSuiteNode"),
                "index.html");
    }

    @Test
    @OperateOnDeployment("testClassNode")
    public void testClassLevel(@ArquillianResource URL webAppUrl) throws IOException {
        assertNotNull(webAppUrl);
        final Request request = new Request.Builder().url(webAppUrl).build();
        assertEquals("testClassNode", new OkHttpClient().newCall(request).execute().body().string());
    }

    @Test
    @OperateOnDeployment("testSuiteNode")
    public void testSuiteLevel(@ArquillianResource URL webAppUrl) throws IOException {
        assertNotNull(webAppUrl);
        final Request request = new Request.Builder().url(webAppUrl).build();
        assertEquals("testSuiteNode", new OkHttpClient().newCall(request).execute().body().string());
    }

    /**
     * Tests if the node injected via {@link WithNode} is running.
     */
    @Test
    public void testInjectionViaWithNode() {
        assertTrue(alpineNode.isPortOpen(8822));
        assertFalse(alpineNode.isPortOpen(8823));
        assertEquals("alpine-ssh", alpineNode.getName());
    }

}
