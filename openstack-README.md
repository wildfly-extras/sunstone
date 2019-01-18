# OpenStack cloud provider implementation

## Prepare

You have to create a keypair in the PEM format, obtain its public part in the OpenSSH public key format
and upload it to OpenStack:

1. `ssh-keygen -t rsa -f ./my-openstack.pem` (use an empty passphrase)
1. You will have two files: `my-openstack.pem` contains the private key and you have to keep it to yourself,
   and `my-openstack.pem.pub` is the public key that you will add to OpenStack.
1. Login to your OpenStack instance, go to _Access & Security_, open the _Key Pairs_ tab
   and click _Import Key Pair_. (Note that the name is misleading. You will only add the _public_ part of the keypair!)
1. Pick an ID (here `my-openstack`) and enter it into _Key Pair Name_. Put the content of file `my-openstack.pem.pub`
   into _Public Key_.

## Begin

Suppose the `my-openstack.pem` file created above is actually located at `/home/my/openstack/my-openstack.pem`.
Also suppose that the URL you use to login to your OpenStack instance is `http://openstack.example.com`.

You also need the username, password and a tenant name (a _project_ in OpenStack parlance).

Finally, you'll also have to pick an image that you want to use. The image will also dictate the user name
you have to use for SSH. See [http://docs.openstack.org/image-guide/obtain-images.html](http://docs.openstack.org/image-guide/obtain-images.html)
for typical user names for images of various operating systems. In this example, suppose that the image is
Red Hat Enterprise Linux and hence the user name is `cloud-user`.

The following set of properties will get you started:

```
cloud.provider.myprovider.type=openstack
cloud.provider.myprovider.openstack.endpoint=http://openstack.example.com:5000/v2.0
cloud.provider.myprovider.openstack.username=PROJECT:USERNAME
cloud.provider.myprovider.openstack.password=PASSWORD

node.mynode.openstack.image=IMAGE-NAME
node.mynode.openstack.ssh.user=cloud-user
node.mynode.openstack.ssh.privateKeyFile=/home/my/openstack/my-openstack.pem
node.mynode.openstack.keyPair=my-openstack
node.mynode.openstack.instance.type=m1.medium
```

## Details

OpenStack cloud provider is based on JClouds `openstack-nova` provider implementation.

### CloudProvider

```
cloud.provider.[name].type=openstack
```

List of OpenStack `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| leaveNodesRunning      | Whether all the started virtual machines should be left running.  | `false`                            |
| openstack.endpoint     | A specific endpoint for connecting within the given region.       | [None. Mandatory.]                 |
| openstack.username     | The username in form `[tenant]:[user]` for your user.             | [None. Mandatory.]                 |
| openstack.password     | The password for your user.                                       | [None. Mandatory.]                 |
| openstack.projectName  | The project name (OS_PROJECT_NAME in OpenStack RC File)           | [None. Mandatory when using Keystone v3 (e.g. `cloud.provider.myprovider.openstack.endpoint=http://openstack.example.com:5000/v3`)] |
| openstack.projectDomainId | The project domain ID (OS_PROJECT_DOMAIN_ID in OpenStack RC File) | [None. Mandatory when using Keystone v3 (e.g. `cloud.provider.myprovider.openstack.endpoint=http://openstack.example.com:5000/v3`)] |
| openstack.userDomainName | The user domain name (OS_USER_DOMAIN_NAME in OpenStack RC File) | [None. Mandatory when using Keystone v3 (e.g. `cloud.provider.myprovider.openstack.endpoint=http://openstack.example.com:5000/v3`)] |

### Node

List of OpenStack `Node` properties:

| Property name                | Description                                                       | Default value                      |
|:-----------------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup                    | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| openstack.instance.type      | Instance type for the node (Flavor). This defines the computing/networking/... capabilities of the node. | [None. Mandatory.] |
| openstack.image              | Name of the image for the node. If ambiguous or missing, `openstack.image.id` will be used. | [None. Mandatory. (Well techinically, it's optional, but it's recommended to treat is as mandatory.)] |
| openstack.image.id           | ID of the image for the node. Used when `openstack.image` is ambiguous or missing. | [None. Mandatory when `openstack.image` is missing or ambiguous.] |
| openstack.bootScript         | Allows you to specify a script that is to be run on boot. The script is run with `sudo`. | [None. Optional.] |
| openstack.bootScript.file    | As `openstack.bootScript`, but allows you to specify a path to a file that contains the script. Only one of `openstack.bootScript` and `openstack.bootScript.file` can be specified at a time. | [None. Optional.] |
| openstack.floatingIpPools    | Comma separated list of floating IP pool names. If the value is not provided, then all available pool names are used. | [None. Optional.] |
| openstack.autoAssignFloatingIp | Automatic Floating Ip assignation (if set to `false` disables Floating Ip assignation also when `openstack.floatingIpPools` is set) | `true` |
| openstack.keyPair            | The key pair name (for public key) to be imported into this instance as an `authorized_key`. You can import a public key in the OpenStack dashboard. | [None. Optional.] |
| openstack.inboundPorts       | Comma separated list of ports that can be used to access the instance. You will usually require at least port 22 for ssh access. | [None. Optional.] |
| openstack.region             | OpenStack Location/Region name. If there is only one region, then the property config is optional. | [None. Mandatory when more regions.] |
| openstack.securityGroups     | The comma separated security group names for the instance.        | [None. Optional.] |
| openstack.networks           | The comma separated list of networks for the instance. Expects network UUIDs (not human-readable names).        | [None. Optional.] |
| openstack.ssh.user           | The user name for accessing the instance via SSH. See [http://docs.openstack.org/image-guide/obtain-images.html](http://docs.openstack.org/image-guide/obtain-images.html) for typical user names for images of various operating systems. | [None. Optional.] |
| openstack.ssh.password       | Overrides the user password for accessing the instance.           | [None. Optional. One of `openstack.ssh.password`, `openstack.ssh.privateKey` or `openstack.ssh.privateKeyFile` should be set.] |
| openstack.ssh.privateKey     | PEM encoded PKCS#8 private key which should be used to connect to the instance. | [None. Optional. One of `openstack.ssh.password`, `openstack.ssh.privateKey` or `openstack.ssh.privateKeyFile` should be set.] |
| openstack.ssh.privateKeyFile | The path to the private key file which should be used to connect to the instance. If special value `default` is used, then private key is loaded from `~/.ssh/id_rsa`. This property is only used when the `openstack.ssh.privateKey` property is empty.| [None. Optional. One of `openstack.ssh.password`, `openstack.ssh.privateKey` or `openstack.ssh.privateKeyFile` should be set.] |
| openstack.userData           | User data in a string. Takes precedence over `openstack.userData.file`. | [None. Optional.] |
| openstack.userData.file      | Path to file with user data.                                      | [None. Optional.] |
