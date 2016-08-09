# WildFly Nodes

The `WildFlyNode` class is a subclass of the `NodeWrapper` class. It lives in `sunstone-wildfly` maven module.

The implementation provides access to WildFly management API for the wrapped Nodes.

**Usage:**

```
node.node0.wildfly.management.port=9990
node.node0.wildfly.management.user=admin
node.node0.wildfly.management.password=pass.1234
```

```java
import org.wildfly.extras.sunstone.api.wildfly.WildFlyNode;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Operations;


try (WildFlyNode wildFlyNode = new WildFlyNode(cloudProvider.createNode("node0"))) {
    try (final OnlineManagementClient client = wildFlyNode.createManagementClient()) {
        Operations ops = new Operations(client);
        System.out.println("WildFly management user: " + ops.whoami().get("result", "identity", "username").asString());
    }
}

```

List of configuration properties for WildFlyNode instances.

| Property name               | Description                                                       | Default value                      |
|:----------------------------|:------------------------------------------------------------------|:-----------------------------------|
| wildfly.management.port     | Management port available on the node. If the wrapped node supports port mapping, the value provided to this property is without the mapping (i.e. private port). | 9990 or 9999 (if 9990 is not open)             |
| wildfly.management.user     | Management user                                                   | [None. Optional.]                  |
| wildfly.management.password | Password of management user                                       | [None. Optional.]                  |
| wildfly.management.portOpeningTimeoutInSec | Timeout (in seconds) used for waiting for management port. | `60`                       |
| wildfly.management.connectionTimeoutInSec  | Timeout (in seconds) used for Creaper's `OnlineManagementClient` operations | `60`      |
| wildfly.management.bootTimeoutInSec  | Timeout (in seconds) for server to finish booting after management port becomes available. | `60`      |
| wildfly.mode                | Operating mode of the server. `standalone` or `domain`.           | `standalone`                       |
| wildfly.domain.default.profile | Will be passed to Creaper's `OnlineOptions.forProfile` if `wildfly.mode` is set to `domain`. | [None. Optional.] |
| wildfly.domain.default.host    | Will be passed to Creaper's `OnlineOptions.forHost` if `wildfly.mode` is set to `domain`.    | [None. Optional.] |

