package org.wildfly.extras.sunstone.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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
import org.wildfly.extras.sunstone.api.Node;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * This class tests if the suite level Nodes defined in the default cloud properties were correctly created.
 *
 */
@RunWith(Arquillian.class)
public class SuiteLevelNodeTest {


    @InjectNode("suite-level-1")
    @ArquillianResource
    private Node suiteNode1;

    @InjectNode("suite-level-2")
    @ArquillianResource
    private Node suiteNode2;


    @Deployment(name = "test1", testable=false)
    @TargetsContainer("suite-level-1")
    public static WebArchive createTestDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test1.war").addAsWebResource(new StringAsset("test1"), "index.html");
    }

    @Deployment(name = "test2", testable=false)
    @TargetsContainer("suite-level-2")
    public static WebArchive createTest2Deployment() {
        return ShrinkWrap.create(WebArchive.class, "test2.war").addAsWebResource(new StringAsset("test2"), "index.html");
    }

    @Test
    @OperateOnDeployment("test1")
    public void test1(@ArquillianResource URL webAppUrl) throws IOException {
        assertNotNull(webAppUrl);
        final Request request = new Request.Builder().url(webAppUrl).build();
        assertEquals("test1", new OkHttpClient().newCall(request).execute().body().string());
    }

    @Test
    @OperateOnDeployment("test2")
    public void test2(@ArquillianResource URL webAppUrl) throws IOException {
        assertNotNull(webAppUrl);
        final Request request = new Request.Builder().url(webAppUrl).build();
        assertEquals("test2", new OkHttpClient().newCall(request).execute().body().string());
    }

    @Test
    public void testInjectedNodes() throws IOException {
        assertNotNull(suiteNode1);
        assertNotNull(suiteNode2);
        assertNotEquals(suiteNode1.getPrivateAddress(), suiteNode2.getPrivateAddress());
    }

}
