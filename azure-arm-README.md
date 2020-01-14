# Microsoft Azure ARM cloud provider

__This cloud provider is not yet ready and is only available as a technical preview!__
Similarly, this document is also not yet ready, though the _Details_ section is (mostly) accurate.

## Prepare

You have to have a Microsoft Live Account. If you don't have one, create it
at [https://signup.live.com/](https://signup.live.com/).

The account must be assigned a subscription. Ask around about that.

Once you have that, you have to create a service principal for OAuth authentication.
The service principal should use a password for auth, _not_ a certificate.
It should have a reasonable role assigned; use _Contributor_ if you're not sure.

See these documents for more information about that:
- [https://azure.microsoft.com/en-us/documentation/articles/resource-group-authenticate-service-principal-cli/](https://azure.microsoft.com/en-us/documentation/articles/resource-group-authenticate-service-principal-cli/)
  for info about generating the service principal
- [https://azure.microsoft.com/en-us/documentation/articles/xplat-cli-install/](https://azure.microsoft.com/en-us/documentation/articles/xplat-cli-install/)
  for info about installing the Azure CLI

## Begin

TODO

## Details

Azure cloud provider is based on JClouds-Labs `azurecompute-arm` implementation.
Note that the underlying JClouds implementation is still heavily work in progress.

### CloudProvider

```
cloud.provider.[name].type=azure-arm
```

List of Azure `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| leaveNodesRunning      | Whether all the started virtual machines should be left running.  | `false`                            |
| azure-arm.subscriptionId | Azure subscription ID                                           | [None. Mandatory.]                 |
| azure-arm.tenantId     | Azure tenant ID. Used for OAuth authentication.                   | [None. Mandatory.]                 |
| azure-arm.applicationId | The application ID of the Azure service principal.               | [None. Mandatory.]                 |
| azure-arm.password     | Password to the service principal. See `azure.applicationId`.     | [None. Mandatory.]                 |
| azure-arm.location | Azure region to be used e.g. eastus |[None. Mandatory.] |
| azure-arm.publishers   | Comma-delimited list of publishers whose marketplace images are recognized. If you want to use a marketplace image, you have to specify its publisher. Please note that listing all images takes a long time (it's only done once, though), so it's not the best idea to add publishers "just in case". | `Canonical,RedHat` |

### Node

List of Azure `Node` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| azure-arm.image        | Reference to the Azure virtual machine image in the format `location/publisher/offer/sku`. Either references a marketplace image (e.g. `eastus/Canonical/UbuntuServer/16.04.0-LTS`) or an image in a blob store under a particular storage account. In the latter case, it looks like `location//#storageAccount/blob`. | [None. Mandatory.] |
| azure-arm.image.isWindows | TODO this is implemented, but I'm not sure if it makes any sense. | `false`                         |
| azure-arm.bootScript   | Allows you to specify a script that is to be run on boot. The script is run with `sudo`. | [None. Optional.] |
| azure-arm.bootScript.file | As `azure-arm.bootScript`, but allows you to specify a path to a file that contains the script. Only one of `azure-arm.bootScript` and `azure-arm.bootScript.file` can be specified at a time. | [None. Optional.] |
| azure-arm.size         | ID of the Azure ARM virtual machine size. Some typical are: `Basic_A[0-4]`, `Standard_A[0-7]`. Note that not all locations have to support all sizes. | Azure-specific default value. |
| azure-arm.inboundPorts | Comma-delimited numbers of TCP ports that should be open.         | 22                                 |
| azure-arm.ssh.user     | Username of the user that will be created in the virtual machine. Will be later used for SSH. | [None. Mandatory.] |
| azure-arm.ssh.password | Password of the user that will be created in the virtual machine. Will be later used for SSH. Note that Azure requires passwords to have certain quality; too simple passwords will cause an error. | [None. Mandatory.] |
| azure-arm.resourceGroup | Resource group, where the given node will be created                                                                                                                                               | [None. Optional.]
