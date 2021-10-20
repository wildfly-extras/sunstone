# Arquillian extension

Sunstone library comes with Arquillian extension for simpler testing in cloud environments.

The extension is able to:
* control Nodes on test suite level
* control Nodes on test class level
* provide support for WildFly/EAP testing by creating
  [remote connector](http://arquillian.org/modules/wildfly-arquillian-wildfly-remote-container-adapter/) configurations

## Quick start

This section shows a simple way how to move Arquillian from testing local managed WildFly into cloud environment.

Users usually have a configuration file `arquillian.xml` on the classpath and its content looks similar to this one:

```xml
<arquillian xmlns="http://jboss.org/schema/arquillian" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">
    <container qualifier="jboss" default="true">
        <configuration>
            <!-- ... properties -->
        </configuration>
    </container>
</arquillian>
```

Let's move the testing into cloud. We'll use Docker instead of real Cloud provider for this simple scenario.

### Add maven dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.wildfly.arquillian</groupId>
        <artifactId>wildfly-arquillian-container-remote</artifactId>
        <version>${version.org.wildfly.arquillian}</version>
    </dependency>
    <dependency>
        <groupId>org.wildfly.extras.sunstone</groupId>
        <artifactId>sunstone-arquillian</artifactId>
        <version>${version.org.wildfly.extras.sunstone}</version>
    </dependency>
</dependencies>
```

WildFly client libraries must also be specified as described
in [Creaper library README](https://github.com/wildfly-extras/creaper/blob/master/README.md#jboss-as-7--wildfly-client-libraries).

**Note:** If you use JUnit and start your tests through Maven surefire plugin, then use the version 2.19.1 or newer. 
Arquillian doesn't work correctly with the older ones due to the issue [SUREFIRE-1187](https://issues.apache.org/jira/browse/SUREFIRE-1187).  

### Add sunstone configuration
Create the file `/sunstone.properties` on the classpath:

```properties
cloud.provider.provider0.type=docker
cloud.provider.provider0.docker.endpoint=http://127.0.0.1:2375/

node.jboss.docker.image=quay.io/jbossqe-eap/wildfly:25.0.0.Final
node.jboss.docker.cmd=sh,-c,$JBOSS_HOME/bin/add-user.sh -u admin -p pass.1234 -r ManagementRealm -g SuperUser && $JBOSS_HOME/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
node.jboss.wildfly.management.port=9990
node.jboss.wildfly.management.user=admin
node.jboss.wildfly.management.password=pass.1234
node.jboss.arquillian.provider=provider0
node.jboss.arquillian.container.register=true
node.jboss.arquillian.container.isDefault=true

arquillian.suite.start.nodes=jboss
```

These properties show how to configure a Node `jboss` which is controlled on a test suite level.
For this Node is created new WildFly remote container configuraion in Arquillian.

The `arquillian.suite.start.nodes` property holds comma separated node/container names which should be controlled
(i.e. created and destroyed) on test suite level.


## Configuration

### Node object properties

| Property name                  | Description                                                                          | Default value      |
|:-------------------------------|:-------------------------------------------------------------------------------------|:-------------------|
| arquillian.provider            | CloudProvider name to be used for given node                                         | [None. Mandatory.] |
| arquillian.container.register  | true/false flag which controls if a WildFly container should be created for the Node | `false`           |
| arquillian.container.isDefault | true/false flag which controls if the created WildFly container is the default one   | `false`           |

The `arquillian.container.register` property is checked for suite level Nodes. 
For class level, if you want to register Node as a container, then use the `@WithWildFlyContainer` annotation instead of `@WithNode`.

The `arquillian.container.isDefault` property has the same meaning as `default` attribute in `container` element within `arquillian.xml`.
This property is only checked for suite level Nodes - it gives some level of protection before unintentional defining more containers as the default one.

### Arquillian suite object properties

Arquillian extension comes with new configuration type `ArquillianObjectType.TESTSUITE`.
The new type has property prefix `arquillian.suite`.

| Property name     | Description                                                                     | Default value     |
|:------------------|:--------------------------------------------------------------------------------|:------------------|
| start.nodes       | Comma separated list of Node names to be created on test suite level            | [None. Optional.] |
| destroy.providers | Comma separated list of CloudProvider names to be destroyed on test suite level | [None. Optional.] |

You can use the `destroy.providers` property to ensure the proper clean-up of Nodes and Providers after the testsuite.
Usually, it is not  needed, because the providers are destroyed automatically after all its Nodes are
closed. You can take the advantage of this property in cases when tests work directly with Sunstone API
and they want to be sure they handle the clean-up correctly.

The object name for the TESTSUITE type doesn't need to be provided. Usually only a single test suite is launched.
So you can work simply with `arquillian.suite.start.nodes` instead of `arquillian.suite.[suiteName].start.nodes`.

### System properties

**`sunstone.arquillian.suite`**

Object name of `ArquillianObjectType.TESTSUITE` to be used for current run. The default is empty name.

Example - define more suites in cloud properties
```
arquillian.suite.start.nodes=node0
arquillian.suite.allNodes.start.nodes=node0,node1,node2
```
and select one through the system property during test execution 

```bash
mvn test -Dsunstone.arquillian.suite=allNodes
```

**`sunstone.arquillian.disable`**

If set then the logic provided by this extension is disabled.

It can be used in cases when you have the extension on classpath, but you don't want it to be active.

```bash
mvn test -Dsunstone.arquillian.disable
```


## Annotations

### Test level lifecycle control

Example: 


```java
@WithNode("postgres")
@WithWildFlyContainer("jboss")
@RunWith(Arquillian.class)
public class NodeTest {

    // ...

}
```

And fragment from related cloud properties can look like:

```
cloud.provider.dockerProvider.type=docker
cloud.provider.dockerProvider.docker.endpoint=http://127.0.0.1:2375/

node.postgres.docker.image=postgres:9.4.5
node.postgres.arquillian.provider=dockerProvider

node.jboss.docker.image=quay.io/jbossqe-eap/wildfly:25.0.0.Final
node.jboss.arquillian.provider=dockerProvider
...
```

**`@WithNode`**

This annotation can be used for automatic Node creation for the test class. The provider in which the node is created must
be defined in `arquillian.provider` object property of a given node.


**`@WithWildFlyContainer`**

Similar to the `@WithNode` as it creates Node for the test class, but in addition it also registers
WildFly container configuration in Arquillian. The node name is used as the container name. 


### Injection support

By using `ArquillianResource` annotation, you can inject `CloudProvider` and `Node` instances to your tests.

```java
@ArquillianResource
@InjectCloudProvider("dockerProvider")
private CloudProvider cloudProvider;

// don't forget to define 'arquillian.provider' object property: node.jboss.arquillian.provider=dockerProvider
@ArquillianResource
@InjectNode("jboss")
private Node jbossNode;
```
