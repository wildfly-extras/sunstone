# Docker cloud provider

## Prepare

* [install Docker engine](https://docs.docker.com/engine/installation/) on a host
* [enable remote access](https://docs.docker.com/engine/quickstart/#bind-docker-to-another-host-port-or-a-unix-socket) to Docker
* [allow access to private registries](https://docs.docker.com/engine/reference/commandline/daemon/#insecure-registries)
  if you need them

### Sample configuration for Linux systems using systemd

Run following commands on a machine where is the Docker Engine installed.
It enables remote access (plain TCP - only for loopback address 127.0.0.1)
on standard port `2375` and it also allows to pull Docker images from all insecure registries.

```bash
# switch to root account
sudo su -

# create override for docker start-script
mkdir /etc/systemd/system/docker.service.d
cat << EOT > /etc/systemd/system/docker.service.d/allow-tcp.conf
[Service]
ExecStart=
ExecStart=/usr/bin/docker daemon -H fd:// -H tcp:// --insecure-registry 0.0.0.0/0
EOT

# reload configuration and restart docker daemon
systemctl daemon-reload
systemctl restart docker

# use Ctrl-D to close the 'root' session
```

If the `-H fd://` Docker daemon parameter doesn't work on your OS,
then try to replace it by `-H unix:///var/run/docker.sock`

Find more details in [Control and configure Docker with systemd](https://docs.docker.com/engine/admin/systemd/) guide.

## Begin

Suppose the `docker.my-company.example` is the host, where you've configured docker daemon with enabled remote access
(using plain TCP).

The following set of properties will get you started:

```
cloud.provider.myprovider.type=docker
cloud.provider.myprovider.docker.endpoint=http://docker.my-company.example:2375/

node.mynode.docker.image=jboss/wildfly:10.0.0.Final
node.mynode.docker.waitForPorts=8080
node.mynode.docker.waitForPorts.timeoutSec=30
```

### TLS protected daemon

If the endpoint, you are connecting to, is [protected by TLS](https://docs.docker.com/engine/security/https/)
then use the `docker.tls.*` properties to configure paths to your SSL/TLS key and certificates (in PEM format).

Example:

```properties
cloud.provider.myprovider.type=docker
cloud.provider.myprovider.docker.endpoint=https://docker.my-company.example:2376/
cloud.provider.myprovider.docker.tls.cert=/home/test/.docker/cert.pem
cloud.provider.myprovider.docker.tls.key=/home/test/.docker/key.pem
cloud.provider.myprovider.docker.tls.ca.cert=/home/test/.docker/ca.pem
```

## Details

Docker cloud provider is based on JClouds-Labs `docker` implementation.

### CloudProvider

```
cloud.provider.[name].type=docker
```

List of Docker `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| leaveNodesRunning      | Whether all the started containers should be left running.        | `false`                            |
| docker.endpoint        | Docker REST API url (e.g. `http://127.0.0.1:2375/`)               | [None. Mandatory.]                 |
| docker.apiVersion      | API version (to be used in REST calls URLs)                       | `1.21`                             |
| docker.tls.cert        | path to users X509 certificate file (docker engine: `--tlscert`)  | [None. Optional.]                  |
| docker.tls.key         | path to users private key file (docker engine: `--tlskey`)        | [None. Optional.]                  |
| docker.tls.ca.cert     | path to CA certificate file  (docker engine: `--tlscacert`)       | [None. Optional.]                  |

### Node

**Important notes:**

* Unlike `docker run` default network mode, the Docker nodes are started with `NetworkMode=host` by default. It means the network stack is shared with the physical host!
* The `copyFileToNode(...)` method is only supported through SFTP, so the SSH server (with SFTP) has to be running in the Docker node and its configuration
has to be provided in  `node.[name].ssh.*` properties. 

List of Docker `Node` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| docker.image           | Image name from which the node is started                         |                                    |
| docker.bootScript      | Allows you to specify a script that is to be run on boot. The script is run with `sudo`. | [None. Optional.] |
| docker.bootScript.file | As `docker.bootScript`, but allows you to specify a path to a file that contains the script. Only one of `docker.bootScript` and `docker.bootScript.file` can be specified at a time. | [None. Optional.] |
| docker.networkMode     | NetworkMode used                                                  | `host`                             |
| docker.capAdd          | Comma separated list of Linux capabilities to be added.           |                                    |
| docker.cmd             | Docker command                                                    |                                    |
| docker.entrypoint      | Docker entrypoint | |
| docker.env             | Environment variables in form `KEY=VALUE` separated by value defined in `docker.env.splitter` |        |
| docker.env.splitter    | Separator RegEx for `docker.env` values                           |  `,`                               |
| docker.inboundPorts    | which ports should be open                                        |                                    |
| docker.portBindings    | Comma separated list of TCP port bindings in form "hostPort:nodePort" |                                |
| docker.privileged      | true/false flag which controls if the container is privileged     | false                              |
| docker.ssh.port        | SSH port number if SSH is installed (not-mapped - i.e. in-container value) |                           |
| docker.ssh.user        | SSH username                                                      |                                    |
| docker.ssh.password    | SSH user password                                                 |                                    |
| docker.ssh.privateKey  | PEM encoded PKCS#8 private key which should be used to connect to the instance. | [None. Optional. One of `docker.ssh.password`, `docker.ssh.privateKey` or `docker.ssh.privateKeyFile` should be set.] |
| docker.ssh.privateKeyFile | The path to the private key file which should be used to connect to the instance. If special value `default` is used, then private key is loaded from `~/.ssh/id_rsa`. This property is only used when the `docker.ssh.privateKey` property is empty.| [None. Optional. One of `docker.ssh.password`, `docker.ssh.privateKey` or `docker.ssh.privateKeyFile` should be set.] |
| docker.volumeBindings  | Comma separated list of volume bindings in form `/hostDir:/containerDir`  |                               |
| docker.cpuShares       | [CPU shares](https://docs.docker.com/engine/reference/run/#cpu-share-constraint) (relative weight) |   |
| docker.memoryInMb      | [Memory limit](https://docs.docker.com/engine/reference/run/#runtime-constraints-on-resources) in megabytes | |
