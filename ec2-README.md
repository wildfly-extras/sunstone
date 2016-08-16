# Amazon EC2 cloud provider

## Prepare

You have to have an AWS account. If you work for a company, ask around if you have a company account.
If you don't have any account, you can create a new one at [http://aws.amazon.com/](http://aws.amazon.com/).

Once you have an account, you need to do a few more things. Keep in mind that AWS console is well
documented, so check the documentation if you don't know where to start.

1. Create an IAM user.
   * This is done from the <username> -> Security Credentials -> Users.
   * Keep "Generate an access key for each user" selected.
   * Save the access key ID and secret access key values. You'll need these values, as they
     are used to access the AWS EC2 cloud provider itself, instead of the normal e-mail/password.
   * You can read more about IAM users at [IAM users documentation](http://docs.aws.amazon.com/IAM/latest/UserGuide/id_users.html?icmpid=docs_iam_console)
1. Create or import a key pair.
   * This is done from the EC2 console -> Network and Security -> Key Pairs.
   * If importing a key pair, make sure you read the requirements that EC2 places on the keypair.
   * The keypair will be used for connecting to your instances.
   * Write down the key-pair name and the location of your key.
   * You can read more about keypairs at [EC2 key-pair documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html?icmpid=docs_ec2_console).
1. Write down your account ID and account IDs of all those whose images you wish to use. These are
   sometimes referred to as owner IDs.
   * Your account ID can be found at your account page: <username> -> My Account.
   * The account IDs for other owners can be most easily found by going into EC2 console -> AMIs.
   * Each AMI has an owner associated with it, this can be found in the Owner column.
1. Write down the AMI ID you want to use.
   * This can be found at EC2 console -> AMIs.

## Begin

We will assume you have your access key ID (ACCESS_KEY_ID) and secret access key (SECRET_ACCESS_KEY)
and that the key for your keypair called KEY_PAIR is located at `/home/me/ec2/ec2-key.pem`. We will assume that
the account/owner IDs you wish to use are OWNER1 and OWNER2. We will assume that your AMI ID is
AMI_ID, the name of the AMI is AMI_NAME and you can call `ssh` and login into the instance
(once it is started) as user EC2_USER.

The following set of properties will get you started:

```
cloud.provider.myprovider.type=ec2
cloud.provider.myprovider.ec2.ami.owners=OWNER1,OWNER2
cloud.provider.myprovider.ec2.region=${ec2.region:-us-east-1}
cloud.provider.myprovider.ec2.endpoint=https://ec2.us-east-1.amazonaws.com
cloud.provider.myprovider.ec2.accessKeyID=ACCESS_KEY_ID
cloud.provider.myprovider.ec2.secretAccessKey=SECRET_ACCESS_KEY

node.mynode.ec2.instance.type=${ec2.instance.type:-t1.micro}
node.mynode.ec2.image=AMI_NAME
node.mynode.ec2.image.id=AMI_ID
node.mynode.ec2.inboundPorts=22
node.mynode.ec2.waitForPorts=22
node.mynode.ec2.waitForPorts.timeoutSec=300
node.mynode.ec2.ssh.privateKeyFile=/home/me/ec2/ec2-key.pem
node.mynode.ec2.keyPair=KEY_PAIR
node.mynode.ec2.securityGroups=default
node.mynode.ec2.ssh.user=EC2_USER
```

## Details

Amazon EC2 cloud provider is based on JClouds `aws-ec2` provider implementation.

## VPCs and subnets

The AWS EC2 cloud provider allows you to assign a node into a subnet. The subnet has to be
specified by it's ID. Every subnet also belongs in a VPC, although a VPC can contain multiple
subnets. There can be two kinds of VPCs and AWS behaves differently depending on which you specify:

* **Default VPC** This is a VPC that is always present in newer AWS accounts or regions. It cannot be
  deleted and every instance that is started without specifying a subnet is assigned inside this
  VPC. When you supply the EC2 cloud provider with an ID of a subnet which belongs in a default
  VPC, you need to specify security groups by names (not IDs!).
* **Non-default VPC.** This is a VPC that has been created by a user in any kind of AWS account. When
  you supply the EC2 cloud provider with a subnet inside a non-default VPC, you need to specify
  security groups by IDs (not names!) due to VPC API requirements.

Sample deployment into a non-default VPC subnet would use properties like these:
```
node.mynode.ec2.subnetId=subnet-sbnt1234
node.mynode.ec2.securityGroupIds=sg-scgrp123
```

The need to specify security groups is temporary and waits on resolution of
https://issues.apache.org/jira/browse/JCLOUDS-1120. When that is resolved, and Sunstone is updated,
a default security group should be created when none is specified.

See the AWS VPC documentation for more details on VPCs: https://aws.amazon.com/documentation/vpc/.

### CloudProvider

```
cloud.provider.[name].type=ec2
```

List of EC2 `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| leaveNodesRunning      | Whether all the started virtual machines should be left running.  | `false`                            |
| ec2.ami.owners         | Comma separated list of owners of AMIs you wish to work with.     | [None. Mandatory.]                 |
| ec2.region             | Region for the cloud provider. See `org.jclouds.aws.domain.Region` for format. | [None. Mandatory.] |
| ec2.logEC2Operations   | Whether to log EC2 operations, like connecting the EC2 cloud provider, reading a list of AMIs available, etc. Good for debugging, but lots of output. | `false` |
| ec2.endpoint           | A specific endpoint for connecting within the given region.       | [Default chosen by `ec2.region`. Optional.]                 |
| ec2.accessKeyID        | The access key ID for your user. Necessary for connecting the cloud provider. Can be found in EC2 console under IAM users. | [None. Mandatory.] |
| ec2.secretAccessKey    | The secret access key for your user. Necessary for connecting the cloud provider. Can be found in EC2 console under IAM users. | [None. Mandatory.] |

### Node

**Important note:**

* Connecting an ssh channel to a new instance may take time - expect several exceptions to be thrown. This is an unfortunate result of an instance being marked as *running* before it is able to handle ssh connections.
* This is also an issue if you are running time consuming tasks in the `user-data`. The instance may be ready before your tasks have finished. Make sure everything you need is up and running.

List of EC2 `Node` properties:

| Property name               | Description                                                       | Default value                      |
|:----------------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup                   | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| ec2.instance.type           | Instance type for the node. This defines the computing/networking/... capabilities of the node. See `org.jclouds.ec2.domain.InstanceType`. | [None. Mandatory.] |
| ec2.instance.name           | The instance name. Has no impact on node configuration.           | [None. Mandatory.] |
| ec2.image                   | Name of the image for the node. If ambiguous or missing, `ec2.image.id` will be used. See EC2 console for the list of instances available to you. | [None. Mandatory. (Well technically, it's optional, but it's recommended to treat is as mandatory.)] |
| ec2.image.id                | ID of the image for the node. Used when `ec2.image` is ambiguous or missing. | [None. Mandatory when `ec2.image` is missing or ambiguous.] |
| ec2.keyPair                 | The key pair for this instance. If you want to connect manually to the instance, this will be used to access it. You can import a key pair in the EC2 console. | [None. Mandatory.] |
| ec2.securityGroups          | The security group definition for your instance. See EC2 console for the list of security groups available to you. | [None. Mandatory.] |
| ec2.inboundPorts            | Comma-separated list of ports that can be used to access the instance. You will usually require at least port 22 for SSH access. | 22 |
| ec2.ssh.user                | Overrides the user name for accessing the instance. Not necessary if you are using a key pair. | [None. Optional.] |
| ec2.ssh.password            | Overrides the user password for accessing the instance. Not necessary if you are using a key pair. | [None. Optional.] |
| ec2.ssh.privateKey          | The SSH private key (not the file of the key!). In case you want to pass a private key manually. Currently unused in favor of `ec2.ssh.privateKeyFile`. | [None. Optional.] |
| ec2.ssh.privateKeyFile      | The path to the private key which should be used to connect to the instance. Necessary for establishing an SSH channel in your tests. | [None. Optional.] |
| ec2.waitForPorts.timeoutSec | How long to wait for ports to open after the instance is started (in seconds). | 300 |
| ec2.waitForPorts            | Comma-separated list of ports to wait for. Port 22 is recommended, since you'll likely want to run some commands through ssh on your machine. | 22 |
| ec2.userData                | User data in a string. Takes precedence over `ec2.userData.file`. | [None. Optional.] |
| ec2.userData.file           | Path to file with user data. See [EC2 User data](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html) | [None. Optional.] |
| ec2.subnetId                | The ID of a subnet this node should belong in. A VPC may have multiple subnets in different availability zones, but a subnet is always only associated with only a single VPC. | [None. Optional.] |
| ec2.securityGroupIds        | The IDs (not names!) of VPC security groups. These are used only for non-default VPCs and cannot be used when security group names are specified. Also see `ec2.subnetId` and `ec2.securityGroups`. | [None. Optional. (Unless subnet ID has been specified.)] |
