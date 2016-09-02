# How to contribute


## Build and run tests

The library is a maven project, so you can simply use `mvn install` command to build it.
Nevertheless the tests requires Docker provider to run tests.
so you have to either disable test run (`-DskipTests` argument)
or run it with providers configured.

Live tests for other cloud providers require configuration and if it's not provided then these tests are skipped.

```bash
mvn clean install -DtestLogToFile=false \
  -Dprovider0=http://docker-host.example.com:2375/ \
  -Dec2.accessKeyID=$EC2_ACCESS_KEY \
  -Dec2.secretAccessKey=$EC2_SECRET_KEY \
  -Dec2.keyPair=$USER \
  -Dec2.ssh.privateKeyFile=$HOME/.ssh/id_rsa \
  -Dec2.subnetId=$SUBNET_ID \
  -Dec2.securityGroupIds=$SECURITY_GROUP_IDS \
  -Dazure.subscriptionId=$AZURE_SUBSCRIPTION_ID \
  -Dazure.privateKeyFile=$AZURE_PRIVATE_KEY_FILE \
  -Dazure.privateKeyPassword=$AZURE_PRIVATE_KEY_PASSWORD \
  -Dazure.image=$AZURE_IMAGE \
  -Dazure.storage=$AZURE_STORAGE \
  -Dazure.ssh.user=$AZURE_SSH_USER \
  -Dazure.ssh.password=$AZURE_SSH_PASSWORD \
  -Dazure-arm.subscriptionId=$AZURE_ARM_SUBSCRIPTION_ID
  -Dazure-arm.tenantId=$AZURE_ARM_TENANT_ID
  -Dazure-arm.applicationId=$AZURE_ARM_APPLICATION_ID
  -Dazure-arm.password=$AZURE_ARM_PASSWORD
  -Dazure-arm.image=$AZURE_ARM_IMAGE
  -Dazure-arm.ssh.user=$AZURE_ARM_SSH_USER
  -Dazure-arm.ssh.password=$AZURE_ARM_SSH_PASSWORD
  -Dopenstack.endpoint=$OS_ENDPOINT \
  -Dopenstack.username=$OS_USERNAME \
  -Dopenstack.password=$OS_PASSWORD \
  -Dopenstack.image.id=9a9f496a-f5c2-4286-81a4-98189a48777a \
  -Dopenstack.ssh.user=cloud-user \
  -Dopenstack.ssh.privateKeyFile=default \
  -Dopenstack.keypair=$USER
```

## Common configuration properties

Some properties are very often used and have the same meaning for all (or majority) of cloud providers.
These should have consistent naming and consistent behavior if at all possible.

Here's a table of these commonly used properties. Note that this is not user documentation; each cloud provider
README has to provide full documentation of the recognized properties. This document servers a developer purpose.

| Property name          | Description                                                       | Node configuration or cloud provider configuration |
|:-----------------------|:------------------------------------------------------------------|:---------------------------------------------------|
| nodegroup              | Name of the node group for this node.                             | both                                               |
| xxx.image              | A human-readable name of the virtual machine image.               | node                                               |
| xxx.image.id           | A unique identifier of the virtual machine image in case the human-readable name can be ambiguous. | node              |
| xxx.inboundPorts       | List of ports that will be exposed to the outside. The default value should be `22`. | node                            |
| xxx.waitForPorts       | List of ports that will be waited for after starting the node. There should be no default value. | node                |
| xxx.waitForPorts.timeoutSec | How long to wait for `xxx.waitForPorts` to become open. The default value can be different for each cloud provider. | node |
| xxx.ssh.port           | SSH port number.                                                  | node                                               |
| xxx.ssh.user           | Username of the user that will be used for SSH.                   | node                                               |
| xxx.ssh.password       | Password of the user that will be used for SSH.                   | node                                               |
| xxx.ssh.privateKey     | Private key for SSH authentication in the PEM format.             | node                                               |
| xxx.ssh.privateKeyFile | Path to a file with private key for SSH authentication in the PEM format. | node                                       |

Current compliance issues:

- Azure doesn't have `azure.ssh.privateKey` and `azure.ssh.privateKeyFile`.
- Docker doesn't have `docker.ssh.privateKeyFile`.
- Only Docker supports `xxx.ssh.port`. This is not that big of an issue, reconfiguring the SSH port number
  seems not _that_ important. Maybe it shouldn't even be listed here as "common".
- EC2 ignores `ec2.ssh.password` and `ec2.ssh.privateKey`.
