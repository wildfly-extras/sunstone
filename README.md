# Sunstone

Simple library which helps to control virtual machines in cloud environments.
It's aimed mainly to testing WildFly application server.

> Name: [Sunstone](https://en.wikipedia.org/wiki/Sunstone_\(medieval\))
> is a crystal that was supposedly used by Vikings to navigate in cloudy weather.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/29c4f841338e4096bd76c5369373044b)](https://www.codacy.com/app/LittleJohnII/sunstone?utm_source=github.com&utm_medium=referral&utm_content=wildfly-extras/sunstone&utm_campaign=badger)
[![Build Status](https://travis-ci.org/wildfly-extras/sunstone.svg?branch=master)](https://travis-ci.org/wildfly-extras/sunstone)

## Motivation

### Why?

*Why yet another library?*  
*Because clouds exist. And they are many! Moreover, everybody loves new libraries, of course.*

The library is here to simplify test development for new clouds. One set of test can be reused for different kind of providers.

### What?

*What does it bring to me?*  
*One view to rule them all.*

* single Java API for all supported cloud providers
* possibility to control WildFly nodes with [Creaper](https://github.com/wildfly-extras/creaper)
* Arquillian test framework support

### How?

*It's so cool. Now, show me the code!*

Add the maven dependency to your project:

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-core</artifactId>
    <version>${version.org.wildfly.extras.sunstone}</version>
</dependency>
```
Define cloud configuration in `my-cloud.properties` property file:

```properties
# Cloud provider - an entrypoint to given cloud
cloud.provider.my-provider.type=docker
cloud.provider.my-provider.docker.endpoint=http://dockerhost0.acme.com:2375/

# Node - a single virtual machine in the cloud 
node.wildfly-node.docker.image=jboss/wildfly:10.0.0.Final
node.wildfly-node.docker.waitForPorts=8080,9990
node.wildfly-node.docker.waitForPorts.timeoutSec=30
```

Then use your favorite Java 8 to do the stuff in the cloud:

```java
import java.io.IOException;
import java.net.URL;

import org.wildfly.extras.sunstone.api.CloudProperties;
import org.wildfly.extras.sunstone.api.CloudProvider;
import org.wildfly.extras.sunstone.api.Node;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * Sunstone quick-start. It creates cloud provider, starts a single node in it and
 * makes HTTP request (port 8080) against the node.
 */
public class App {

	public static void main(String[] args) throws IOException {
		CloudProperties.getInstance().load("my-cloud.properties");
		try (CloudProvider cloudProvider = CloudProvider.create("my-provider")) {
			try (Node node = cloudProvider.createNode("wildfly-node")) {
				final URL url = new URL("http", node.getPublicAddress(), 8080, "");
				final OkHttpClient client = new OkHttpClient();
				final Request nodeNameRequest = new Request.Builder().url(url).build();
				System.out.println("Response from " + url);
				System.out.println(client.newCall(nodeNameRequest).execute().body().string());
			}
		}
	}
}
```

## Clouds support

Available:
* [Docker](docker-README.md)
* [Microsoft Azure](azure-README.md) and, as a technical preview, [Microsoft Azure (ARM)](azure-arm-README.md)
* [Amazon EC2](ec2-README.md)
* [OpenStack](openstack-README.md)
* [Bare Metal](baremetal-README.md) (not really a cloud provider per se, but useful anyway)

## WildFly/EAP support

* [WildFlyNode wrapper](wildfly-README.md)
* [Arquillian extension](arquillian-README.md)

## Logging

SLF4J is used as a logging facade, so you have to have the appropriate adapter on the classpath. If you use Logback,
you don't have to do anything. For other loggers, see [the SLF4J manual](http://www.slf4j.org/manual.html).

The loggers are called `sunstone.*`, short and clear. (For example: `sunstone.core`, `sunstone.core.ssh` etc.)

## Abstraction levels

The library is build around 2 main interfaces:
* `CloudProvider` - represents an entry point to given cloud and is able to control Nodes in it
* `Node` - virtual machines (or containers) in the given cloud

### Cloud provider

Cloud providers are controllers of (or entrypoints to) the given cloud. They are configured through properties prefixed with
**`cloud.provider.[name].`**

Example:

```properties
cloud.provider.provider0.type=docker
cloud.provider.provider0.docker.endpoint=http://127.0.0.1:2375/
```

A new cloud provider is created by using a factory class. 
The `CloudProvider` interface extends `AutoCloseable` so it's a good practice to use it in the try-with-resource block.

```java
import org.wildfly.extras.sunstone.api.CloudProvider;

try (CloudProvider cloudProvider = CloudProvider.create("provider0")) {
    // work with the cloudProvider here
}
```

The most important feature of cloud providers is their ability to control nodes (virtual machines) in the cloud.

```java
import org.wildfly.extras.sunstone.api.Node;


// create nodes
Node nodeA = cloudProvider.createNode("myNodeName");
Node nodeB = cloudProvider.createNode("anotherNodeName");

// or list nodes
List<Node> allNodes = cloudProvider.getNodes();
```

There is one general configuration option for cloud providers - **`cloud.provider.[name].type`** which selects cloud provider implementation.
For more configuration options consult documentation of specific cloud implementation. 


### Node

The `Node` interface represents a single virtual machine in the cloud. Nodes are configured through properties prefixed with
**`node.[name].`**. For instance

```properties
node.node0.docker.image=jboss/wildfly:10.0.0.Final
node.node0.docker.waitForPorts=8080
node.node0.docker.waitForPorts.timeoutSec=30
```

Node creation is controlled by cloud providers. 
The `Node` interface also extends `AutoCloseable` so it's again a good practice to use it in the try-with-resource block.

```java
try (Node node = cloudProvider.createNode("node0")) {
    // work with the node here
}
```

The Node interface provides set of methods to work with it. It supports for instance:

* executing commands on the node
* copying files from/to node
* controlling running state of the node (`start`, `stop`, `kill`)
* retrieving the node address
* checking if a port is open on the node

List of general `Node` properties:

| Property name    | Description                                                                              | Default value     |
|:-----------------|:-----------------------------------------------------------------------------------------|:------------------|
| waitForPorts     | What ports (comma separated list) to wait for. SSH port is recommended, since you'll likely want to run some commands through ssh on the Node. | [None. Optional.] |
| waitForPorts.timeoutSec | How long to wait for ports to open after the instance is started (in seconds).    | 60                |
| bootScript       | Allows you to specify a script that is to be run on boot.                                | [None. Optional.] |
| bootScript.file  | As `bootScript`, but allows you to specify a path to a file that contains the script. Only one of `bootScript` and `bootScript.file` can be specified at a time. | [None. Optional.] |
| bootScript.withSudo | Flag (`true`/`false`) which controls if the boot script runs with sudo.               | `true`            |
| bootScript.remotePath | Path on the Node, where the bootScript should be stored.                            | `"/tmp/onBootScript.sh"` |
| bootScript.waitForPorts | What ports (comma separated list) to wait for **before** the executing `bootScript`. This property is not used if no `bootScript` (or `bootScript.file`) is provided. | [None. Optional.] |
| bootScript.waitForPorts.timeoutSec | How long to wait for ports to open before the bootscript is executed (in seconds). | 60    |
| ssh.fixSudoers   | Flag (`true`/`false`) which controls if disabling `requiretty` option is requested for `/etc/sudoers` file. | false  |
| start.timeoutSec | How long to wait for node start (in seconds).                                            | 300               |
| stop.timeoutSec  | How long to wait for node stop (in seconds).                                             | 300               |
| sudo.command     | Sudo command to be used for `ExecBuilder` executions when `withSudo()` is used.          | `sudo -S`         |

These properties can be overriden on cloud provider level by appending provider name prefix (e.g. `docker.bootScript`).

Consult documentation of specific cloud implementation for Node configuration options. 

#### Node API example

Tar a folder on a Node and copy it to local filesystem:

```java
ExecResult execResult = node.exec(
		"tar",
		"-czvf",
		"/tmp/wildfly-logs.tgz",
		"/opt/jboss/wildfly/standalone/log");

if (execResult.getExitCode() == 0) {
	node.copyFileFromNode("/tmp/wildfly-logs.tgz", Paths.get("wildfly-logs.tgz"));
}
```

### NodeWrapper

Special implementation of the `Node` interface is the `NodeWrapper` class. 

It provides a convenient implementation of the Node interface that can be subclassed by developers wishing to adapt behavior or
provide additional functionality. This class implements the Wrapper or Decorator pattern. Methods default to calling through
to the wrapped request object.

## Configuration

Singleton `CloudProperties` is used to hold library configuration. It allows to load properties from files on system path or classpath.
If a classpath resource `/sunstone.properties` exists, it is loaded by default.

```java
import org.wildfly.extras.sunstone.api.CloudProperties;

CloudProperties.getInstance().load("/org/jboss/cloud.properties").load("/opt/sunstone/wildfly-nodes.properties");

// TODO work with sunstone 

CloudProperties.getInstance().reset();
```

Properties configured in `CloudProperties` can be overridden in several ways. Users can choose their preferred way.

### Configuration templates

You can use existing object configuration as a template for new objects of the same type.

Use property name **`[objectType].[objectName].template`** to reference the template.

```properties
# define a node
node.nodeA.docker.image=jboss/wildfly
node.nodeA.docker.waitForPorts=8080
node.nodeA.docker.waitForPorts.timeoutSec=30

# use the nodeA configuration as a template for a new node
node.nodeB.template=nodeA
node.nodeB.docker.cmd=/opt/jboss/wildfly/bin/standalone.sh,-b,0.0.0.0

# it's possible to use more template levels and override properties from template
node.nodeC.template=nodeB
node.nodeC.docker.waitForPorts=9990
```

### Single object properties

Properties for a single object can also be provided in a property file which contains entries without `[objectType].[objectName].`
prefix. This file is then referenced from `[objectType].[objectName].properties` entry.

For instance you can have a property file `/home/test/wildfly-node.properties` which contains:

```properties
docker.image=jboss/wildfly
docker.waitForPorts=8080
docker.waitForPorts.timeoutSec=30
```

Then property file used in `CloudProperties` configuration could contain:

```properties
# reference external configuration file
node.node0.properties=/home/test/wildfly-node.properties

# use external configuration and override some values
node.node1.properties=/home/test/wildfly-node.properties
node.node1.waitForPorts=9990
```

### Overriding properties

When the library searches a configuration entry, it goes through following levels.

1. lookup entry with given name in **System Properties**
1. lookup entry with given name in **`CloudProperties`**
1. lookup entry located in property file referenced through single object properties `[objectType].[objectName].properties`
1. lookup entry located in the object template (go through the all levels from the beginning)

Once the entry is found it's used (i.e. following levels are skipped).

For instance if properties for your tests contains 

```properties
node.nodeA.docker.image=jboss/wildfly
node.nodeA.docker.waitForPorts=8080
node.nodeA.docker.waitForPorts.timeoutSec=30
```

then you can use system property to quickly override some value:

```bash
mvn test "-Dnode.nodeA.docker.image=jboss/wildfly:10.0.0.Final"
```

### Property replacement

You can also use system-properties replacement directly in property values.
The system property name and default value are separated by a colon `:`. 

It means you can refer system properties directly in property file:

```properties
# try to find the value in "wildfly.image" system property. If it doesn't exist, then use default value "jboss/wildfly"
node.nodeA.docker.image=${wildfly.image:jboss/wildfly}
```

### File paths

Some properties are expected to contain file paths. This is typical e.g. for SSH private keys:

```properties
node.nodeA.openstack.ssh.privateKeyFile=/home/me/.ssh/id_rsa
```

In such cases, it is possible to load data also from classpath, using the `classpath:` prefix:

```properties
node.nodeA.openstack.ssh.privateKeyFile=classpath:org/jboss/test/openstack/ssh-private-key.pem
```

The classpath resource path must be absolute. It must _not_ begin with `/` (see the javadoc of `Class.getResource`
and `ClassLoader.getResource` to see the difference). Note that these classpath resources are actually copied
to the filesystem and the resulting temporary files are scheduled to be deleted at JVM exit.
This shouldn't be a concern typically.

## License

* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
