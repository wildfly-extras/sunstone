# Bare Metal cloud provider

## Prepare

You just have to have a bunch of machines reachable via SSH. If you authenticate to the SSH server
using private keys, you have to have the private key in the PEM format (which is typical).

## Begin

Suppose there are 2 machines you want to use, reachable via `machine1.example.com` and `machine2.example.com`.
Also suppose that both machines have a `cloud-user` user that is accessible via SSH using private key
located at `/home/my/baremetal/my-baremetal.pem`.

The following set of properties will get you started:

```
cloud.provider.myprovider.type=baremetal
cloud.provider.myprovider.baremetal.nodes=machine1, machine2

node.machine1.baremetal.host=machine1.example.com
node.machine1.baremetal.ssh.user=cloud-user
node.machine1.baremetal.ssh.privateKeyFile=/home/my/baremetal/my-baremetal.pem
node.machine1.baremetal.waitForPorts=22

node.machine2.template=machine1
node.machine2.baremetal.host=machine2.example.com
```

## Details

Bare Metal cloud provider is based on JClouds `byon` implementation.

Note that nodes are configured upfront, when the cloud provider is being created,
so passing property overrides to `CloudProvider.createNode()` won't work.

### CloudProvider

```
cloud.provider.[name].type=baremetal
```

List of Bare Metal `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| baremetal.nodes        | Comma-separated list of identifiers of all nodes. This needs to be provided upfront. | [None. Mandatory.] |

### Node

List of Bare Metal `Node` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| baremetal.host         | Host name or IP address of the node.                              | [None. Mandatory.]                 |
| baremetal.ssh.user     | Username of the user that will be used for SSH.                   | [None. Mandatory.]                 |
| baremetal.ssh.password | Password of the user that will be used for SSH.                   | [None. One of `password`, `privateKey`, `privateKeyFile` is mandatory.] |
| baremetal.ssh.privateKey | Private key for SSH authentication in the PEM format.           | [None. One of `password`, `privateKey`, `privateKeyFile` is mandatory.] |
| baremetal.ssh.privateKeyFile | Path to a file with private key for SSH authentication in the PEM format. | [None. One of `password`, `privateKey`, `privateKeyFile` is mandatory.] |
| baremetal.bootScript   | Allows you to specify a script that is to be run on boot. The script is run with `sudo`. | [None. Optional.] |
| baremetal.bootScript.file | As `baremetal.bootScript`, but allows you to specify a path to a file that contains the script. Only one of `baremetal.bootScript` and `baremetal.bootScript.file` can be specified at a time. | [None. Optional.] |
| baremetal.waitForPorts | Comma-separated list of ports that must be open at the beginning. | [None. Optional.]                  |
| baremetal.waitForPorts.timeoutSec | How long to wait for `baremetal.waitForPorts` to become open. | 30 seconds                  |
