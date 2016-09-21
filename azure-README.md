# Microsoft Azure cloud provider

## Prepare

You have to have a Microsoft Live Account. If you don't have one, create it
at [https://signup.live.com/](https://signup.live.com/).

The account must be assigned a subscription. Ask around about that.

Once you have that, you have to create a keypair in the PKCS#12 format, create a certificate in the PEM format
and upload it to Azure. Using Java `keytool`, it's as simple as this:

1. Pick an ID (here `my-azure`), human readable name (here `My Azure`) and a password (here `foobar`).
1. `keytool -genkey -alias my-azure -keystore ./my-azure.pfx -storepass foobar -validity 3650 -keyalg RSA -keysize 2048 -storetype pkcs12 -dname "CN=My Azure"`
1. `keytool -export -alias my-azure -storetype pkcs12 -keystore ./my-azure.pfx -storepass foobar -rfc -file ./my-azure.cer`
1. You will have two files now: `my-azure.pfx` contains the private key and you have to keep it to yourself,
   and `my-azure.cer` is the certificate that you will upload to Azure.
1. Log in to Azure at [https://manage.windowsazure.com/](https://manage.windowsazure.com/)
1. Open _Settings_ (in the left navigation bar at the very bottom). Notice you are on the _Subscriptions_ tab
   (see the top of the main work area). There, you can find the subscription ID -- you will need it.
1. Open the _Management Certificates_ tab, then click the _Upload_ button (in the bottom action bar).
   Select `my-azure.cer` file.

See these documents for more information about the steps above:

- [https://azure.microsoft.com/en-us/documentation/articles/java-create-azure-website-using-java-sdk/#create-a-management-certificate-for-azure](https://azure.microsoft.com/en-us/documentation/articles/java-create-azure-website-using-java-sdk/#create-a-management-certificate-for-azure)
  for info about generating the certificate
- [https://azure.microsoft.com/en-us/documentation/articles/azure-api-management-certs/](https://azure.microsoft.com/en-us/documentation/articles/azure-api-management-certs/)
  for info about uploading the certificate

## Begin

Suppose the `my-azure.pfx` file created above is actually located at `/home/my/azure/my-azure.pfx`.

You also need the subscription ID.

The following set of properties will get you started:

```
cloud.provider.myprovider.type=azure
cloud.provider.myprovider.azure.subscriptionId=YOUR-SUBSCRIPTION-ID
cloud.provider.myprovider.azure.privateKeyFile=/home/my/azure/my-azure.pfx
cloud.provider.myprovider.azure.privateKeyPassword=foobar

node.mynode.azure.image=b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-14_04_3-LTS-amd64-server-20160114.5-en-us-30GB
node.mynode.azure.storageAccountName=ubuntul6r76y6r
node.mynode.azure.ssh.user=myuser
node.mynode.azure.ssh.password=a1.B2,c3.D4
```

## Details

Azure cloud provider is based on JClouds-Labs `azurecompute` implementation.

### CloudProvider

```
cloud.provider.[name].type=azure
```

List of Azure `CloudProvider` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for all nodes in this cloud provider. Should provide information about who started the nodes and shouldn't be prone to collisions. Default value should typically be satisfactory. | Based on current runtime environment. |
| leaveNodesRunning      | Whether all the started virtual machines should be left running.  | `false`                            |
| azure.subscriptionId   | Azure subscription ID                                             | [None. Mandatory.]                 |
| azure.privateKeyFile   | Path to a file that contains a private key in the PKCS#12 format. The corresponding file with a public key and a certificate must be added to Azure management certificate list as described at the beginning of this document. | [None. Mandatory.] |
| azure.privateKeyPassword | Password to the private key. See `azure.privateKeyFile`.        | [None. Mandatory.] |

### Node

List of Azure `Node` properties:

| Property name          | Description                                                       | Default value                      |
|:-----------------------|:------------------------------------------------------------------|:-----------------------------------|
| nodegroup              | Name of the node group for this node. Default value should typically be satisfactory. | The `nodegroup` value from the cloud provider. |
| azure.image            | Name of the Azure virtual machine image                           | [None. Mandatory.]                 |
| azure.image.isWindows  | Set the `true` value if the OS of provided image is Windows. This flag allows to configure username and password for Windows systems. | `false`                            |
| azure.bootScript       | Allows you to specify a script that is to be run on boot. The script is run with `sudo`. | [None. Optional.] |
| azure.bootScript.file  | As `azure.bootScript`, but allows you to specify a path to a file that contains the script. Only one of `azure.bootScript` and `azure.bootScript.file` can be specified at a time. | [None. Optional.] |
| azure.provisionGuestAgent | Allows to set optional `true`/`false` Azure Role `ProvisionGuestAgent` parameter. Set it `true`, if you want to have possibility to reset user's password on Windows VM through the Azure portal. | [None. Optional.] |
| azure.size             | ID of the Azure virtual machine size. Some typical are: `BASIC_A[0-4]`, `A[5-11]`, `EXTRASMALL`, `SMALL`, `MEDIUM`, `LARGE`, `EXTRALARGE`. | Azure-specific default value. |
| azure.inboundPorts     | Comma-delimited numbers of TCP ports that should be open.         | 22                                 |
| azure.storageAccountName | Name of the storate account used for allocating storage for a virtual machine. | [None. Mandatory.]  |
| azure.ssh.user         | Username of the user that will be created in the virtual machine. Will be later used for SSH. | [None. Mandatory.] |
| azure.ssh.password     | Password of the user that will be created in the virtual machine. Will be later used for SSH. Note that Azure requires passwords to have certain quality; too simple passwords will cause an error. | [None. Mandatory.] |
| azure.virtualNetwork   | Name of an existing virtual network in Azure. This node will join this virtual network and will be available in a subnet of this virtual network specified by `azure.subnet`. Both of `azure.virtualNetwork` and `azure.subnet` must be used, or none of them. Specifying only one is an error. | [None. Optional.] |
| azure.subnet           | Name of an existing subnet in an existing virtual network in Azure. See `azure.virtualNetwork` for more. | [None. Optional.] |
