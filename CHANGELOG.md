# Changelog

## 2.1.0-SNAPSHOT (not yet released)

## 2.0.0 (2021-10-20)
- Breaking change!
- removed JClouds
- support for Azure ARM templates & AWS CloudFormation templates
- support injection for various Azure and AWS resources
- support WildFly deployment to various cloud resources

## 1.2.3 (2021-10-20)
- updated JClouds to 2.4.0
- added `azure-arm.planPublisher`, `azure-arm.planName`, `azure-arm.planProduct` properties which allow you to create BYOS VM
- fixed issues with image URLs
- migrate to okhttp3

## 1.2.2 (2020-01-23)
- switched experimental Azure ARM provider to non-experimental
- added `azure-arm.location` which allows to specify Azure region
- added `azure-arm.resourceGroup` which allows to specify the resource group where a node should be created
- fixed recent issues with Travis CI builds

## 1.2.1 (2019-03-15)
- added `openstack.socketFinderAllowedInterfaces` to the configurable options for OpenStack provider
- fixed OpenStack support for installations with routable private addresses (i.e. where no floating IPs are assigned)

## 1.2.0 (2019-03-05)
- added `openstack.projectName`, `openstack.projectDomainId` and `openstack.userDomainName` to the configurable options for OpenStack provider, in order to authenticate when using Keystone v3 API
- added `openstack.autoAssignFloatingIp` to the configurable options for OpenStack provider

## 1.1.0 (2019-01-09)
- added `ExecBuilder#exec` method with configurable timeout
- added `ExecBuilderFactory` that allows to create preconfigured `ExecBuilder`
- added `templateTo` property on nodes to allow inverted creation of copies of nodes
- added `openstack.networks` to the configurable options for OpenStack provider
- fixed issues with the testsuite on JDK 9+
- fixed two property names starting with `clouds` and not `sunstone`

## 1.0.0 (2017-01-06)

- removed properties and methods which were marked as deprecated
- updated `RELEASE_PROCEDURE.md` to reflect Sonatype deployments

## 0.10.0 (2016-12-05)

- added `sudo.command` Node property which allows to redefine sudo command used
  in `ExecBuilder`
- removed automatic fixing `/etc/sudoers` file when an SSH client instance is requested from Node
  - added `ssh.fixSudoers` Node property which enables the logic
- removed support for deprecated Azure Node properties `azure.userData`
  and `azure.userData.file`; use the `bootScript` and `bootScript.file` instead
- unified `waitForPorts` and `waitForPorts.timeoutSec` properties across providers
- extended `bootScript.*` configuration
  - added possibility to wait for ports before a boot script is executed; it's configured
    by `bootScript.waitForPorts` and `bootScript.waitForPorts.timeoutSec` properties
  - added `bootScript.withSudo` property which allows to disable sudo for the bootScript
  - remote script path on Node made configurable through `bootScript.remotePath` property
- added `docker.ssh.privateKeyFile` which allows to specify path to the private key file
  which is used to connect to the instance via ssh.
- use public JClouds release `2.0.0`

## 0.9.0 (2016-09-06)

- added new properties shared across cloud providers; These properties can be overriden
  by provider specific variants prefixed with `[providerType].` (e.g. `docker.bootScript`):
  - `bootScript` and `bootScript.file`
  - `stop.timeoutSec` and `start.timeoutSec`
- added support for Docker volume bindings; It's configured through a new `DockerNode`
  property `docker.volumeBindings` which accepts comma separated list of bindings
- added new configuration entries to Bare Metal provider:
  - `baremetal.ssh.port` - port number of SSH server in the Node
  - `baremetal.privateAddress` - configurable value for `getPrivateAddress()` method
- added configurable boot timeout for WildFly - `wildfly.management.bootTimeoutInSec` property
- added support for subnets on EC2, switched on by providing `ec2.subnetId` and
  `ec2.securityGroupIds`; note that security group IDs have to be provided, not names
- added more restrictive permissions to the temporary files created for path resources 
  with `classpath:` prefix (if the underlying OS supports configuring the permissions)
- `CloudProperties` now throw `ResourceLoadingException` when loading properties from a
  source that does not exist; the thrown excepion wraps its cause
- fixed preliminary closing of SSH client when using `asDaemon` method of the `ExecBuilder`
- WildFly 10.1.0 BOM replaces JBoss EAP 6.4 BOM in `dependencyManagement`
- removed transitive dependency on JBoss Controller client; users have to define it manually
  in the same way as when using standalone Creaper library
- fixed issues reported by Coverity static analysis tool
- use JClouds internal fork `2.0.0-eapqe.12` - with Azure VM disk delete fix included

## 0.8.0 (2016-08-03)

- added support for non blocking creation of nodes in `CloudProvider`
  via newly introduced methods `createNodeAsync(...)` and `createNodes(...)`;
  the `createNodes` method returns instance of new interface `CreatedNodes` which extends
  `AutoCloseable` and therefore can be used in try-with-resource blocks
- the Arquillian extension starts and stops nodes asynchronously
- added `azure.virtualNetwork` and `azure.subnet` to allow multiple nodes
  to join a single common virtual network
- introduced new facory method `fromShellScript` in `ExecBuilder`
- added `ExecBuilder` support for configuring environment by `environmentVariable(...)`
  and `environmentVariables(...)` methods
- added `ConfigProperties` and `Node.config()`/`CloudProvider.config()`;
  the `Node.getProperty()` and `CloudProvider.getProperty()` methods are now
  deprecated and scheduled for removal
- added Azure ARM cloud provider, as a technical preview
- fixed closing nodes created by Arquillian extension when `@WithWildFlyContainer` is used
- use JClouds internal fork `2.0.0-eapqe.10` - with snakeyaml version update

## 0.7.0 (2016-07-21)

- _Clouds_ renamed to **Sunstone**: Maven `groupId` is now
  `org.wildfly.extras.sunstone`, Maven `artifactId`s are now
  `sunstone-*`, package names are now `org.wildfly.extras.sunstone.*`,
  logger names are now `sunstone.*` (these changes are breaking),
  default configuration file is now `/sunstone.properties`
  (the older `/clouds.properties` and `/org/wildfly/extras/clouds/default.properties`
  are still recognized, but their support is deprecated and scheduled for removal)
- improved **performance of the Azure** cloud provider
- fixed resource leak in `SshClient`
- added option `leaveNodesRunning` to all cloud providers
  except of bare metal (where it doesn't make sense)
- added support for "user-data" to Azure cloud provider, mimics EC2 user-data
  functionality
- added configurable timeouts for management interface in `WildFlyNode`
  (properties `wildfly.management.connectionTimeoutInSec`,
  `wildfly.management.portOpeningTimeoutInSec`)
- added optional Azure node configuration which allows to set VM Agent provisioning
  (property `azure.provisionGuestAgent`)
- removed project dependency on `jclouds-log4j`
- use JClouds internal fork `2.0.0-eapqe.9` - improved Azure performance
- use Creaper `1.4.0`

## 0.6.1 (2016-07-01)

- fixes wrong parent version in clouds library modules

## 0.6.0 (2016-07-01)

- instead of the `/org/wildfly/extras/clouds/default.properties`
  classpath resource, `/clouds.properties` is expected now
  (the old path still works, but is deprecated and scheduled for removal)
- all file-related properties now accept the `classpath:` prefix
- added `RedirectMode` support to `ExecBuilder.redirect*` methods,
  it newly allows to append output to existing file
- value delimiter in cloud properties system property value expressions
  changed from ":-" to ":"; It can be newly configured by setting 
  `clouds.sysprop.value.delimiter` system property
- added `CloudProvider.getProperty`, similarly to `Node.getProperty`
- added `ExecResult.assert*` assertion methods
- Azure now accepts properties `azure.ssh.user` and `azure.ssh.password`
  (the old `azure.login.name` and `azure.login.password` are still recognized,
  but deprecated and scheduled for removal)
- fixed an issue where too long computer name was generated on Azure
- Azure no longer adds `22` to the list of inbound ports if it's missing
- fixed node name creation for Docker
- use JClouds internal fork `2.0.0-eapqe.7` - handles EC2 VPC issues

## 0.5.1 (2016-06-01)

- added Azure Node property `azure.image.isWindows` (true/false), it allows
  to set username/password for Windows VM

## 0.5.0 (2016-06-01)

- added a Bare Metal cloud provider (not really cloud...)
- added `Node.ssh` for advanced usecases - it returns SSH client for the Node
- reworked Arquillian extension
  - added declarative way to specify suite-level Nodes
  - introduced object type `arquillian.suite`, it allows to define Nodes to be
    started for the suite and providers to be destroyed when the suite finishes
  - annotations were changed: `@WildFlyContainer` was removed, `@WithNode` and
    `@WithWildFlyContainer` were added. The new annotations are designed only
    for controlling of class level nodes and containers.
  - the injection of `Node` or `CloudProvider` by using `@ArquillianResource`
    annotation doesn't create the instance if it doesn't exist already
- improved Docker provider
  - added `docker.tls.*` configuration options to `DockerCloudProvider`,
    they allow to configure access to TLS protected Docker endpoints
  - added `docker.privileged` and `docker.capAdd` configuration into
    `DockerNode`, it allows for instance to use `iptables` in containers
  - added `docker.apiVersion` to `DockerCloudProvider` configuration,
    it allows to control version in REST calls URL
- added `NodeWrapper` class into the `core` module, the `WildFlyNode` newly
  inherits from the `NodeWrapper`
- improved logging; all loggers now begin with `clouds.`
- `Node.exec`, `Node.copyFileFromNode`, `Node.copyFileToNode`
  and `ExecBuilder.exec` now throw more checked exceptions
- fixed copying files on Azure
- info about the Arquillian extension moved to `arquillian-README.md`
- info about `WildFlyNode` moved to `wildfly-README.md`
- use JClouds internal fork `2.0.0-eapqe.6` - handles Docker configuration issues

## 0.4.0 (2016-04-29)

- split project into 3 Maven modules `core`, `wildfly` and `arquillian`;
  you have to replace the dependency on `org.wildfly.extras.clouds:clouds`
  by dependencies on `clouds-core`, `clouds-wildfly` and `clouds-arquillian`
- added properties for configuring server operating mode for `WildFlyNode`
  (`standalone` or `domain`)
- added advanced support for command execution on `Node`s:
  `ExecBuilder` class allows to configure the commands to run
  on background (`nohup`) and/or with root privileges (`sudo`)
- `CloudProvider.Factory.createCloudProvider` methods are now deprecated,
   suggested replacement being `CloudProvider.create`
- image lookup for EC2 changed: `ec2.image` containing a readable
  name should be preferred to `ec2.image.id`, but `ec2.image.id`
  must still be used when `ec2.image` is ambiguous (or missing)
- image lookup for OpenStack changed: `openstack.image` containing a readable
  name should be preferred to `openstack.image.id`, but `openstack.image.id`
  must still be used when `openstack.image` is ambiguous (or missing)
- improved the default JClouds node group and made it configurable
  on a per-cloud-provider basis (`cloud.provider.xxx.nodegroup`)
  and per-node basis (`node.xxx.nodegroup`)
- improved selection of default floating IP pools in the OpenStack provider
  and made it configurable for each node (`openstack.floatingIpPools`)
- added `openstack.ssh.privateKey` Node property in the `openstack` provider
  PEM encoded PKCS#8 private key can be used as a value for this property
- added `CloudProperties.load(Class)` method to simplify loading property files
- use JClouds internal fork `2.0.0-eapqe.5` - handles problem in Azure
  Nodes lifecycle
- switched the SSH client library from SSHJ to JSCH

## 0.3.0

- use JClouds internal fork `2.0.0-eapqe.4`
- add OpenStack provider
- improve all the READMEs
- `Node.waitForPorts` now signalizes that port is unreachable
  by throwing `PortOpeningTimeoutException`
- introduce a common implementation of file copying
- introduce a common "TCK" for cloud providers (`AbstractCloudProviderTest`)
- refactoring of all cloud provider implementations to reduce
  code duplication and simplify implementing a new provider

## 0.2.1

- bug fixes and improvements in the EC2 provider
- bug fixes in the Arquillian extension
- remove deprecated properties from the Docker provider

## 0.2.0

- use JClouds internal fork `2.0.0-eapqe.2`
- add Amazon EC2 provider
- add Microsoft Azure provider
- `Node.exec` now takes `String...` instead of `List<String>`
- restructure the READMEs

## 0.1.4

- fix the Arquillian extension so that `@WildFlyContainer` can be repeated
- use Checkstyle with the WildFly configuration to maintain code style

## 0.1.3

- add `Node.getPrivateAddress`
- workaround for `server-still-booting` failures in `WildFlyNode`

## 0.1.2

- add `Node.waitForPorts`
- add `WildFlyNode.waitUntilRunning`

## 0.1.1

- bug fixes in the Docker provider

## 0.1.0

- initial release based on JClouds internal fork `2.0.0-eapqe.1`
- basic `CloudProvider` and `Node` API
- add `WildFlyNode`, a WildFly wrapper for `Node`
- Arquillian extension
- Docker provider
