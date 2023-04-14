# Azure

This guide explains how to develop tests.

## Prerequisites

You need to have programmatic access to Azure, which means you have created a service principal.

## Begin

Add the following properties into `sunstone.properties` located in your resources' folder:

```properties
sunstone.azure.subscriptionId=${azure.subscriptionId}
sunstone.azure.tenantId=${azure.tenantId}
sunstone.azure.applicationId=${azure.applicationId}
sunstone.azure.password=${azure.password}
sunstone.azure.region=${azure.region:eastus2}
sunstone.azure.group=${azure.group}

sunstone.wildfly.mgmt.port=9990
sunstone.wildfly.mgmt.user=admin
sunstone.wildfly.mgmt.password=pass.1234
sunstone.wildfly.mgmt.connection.timeout=120000
```

Mind that only subscription id, tentant id, application id and password are mandatory. Everything else you can also set in the annotations.

Add the following dependency:

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-azure</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

## Test development

This chapter provides a guide and explains the logic behind cloud deployment, injection and WildFly deploy operation. Please see JavaDoc for relevant annotations.

### Cloud deployment

Only Azure ARM templates are supported for deploying resources to Azure:

```java
@WithAzureArmTemplate(
        parameters = @Parameter(k = "vnetName", v = AzureTestConstants.VNET_NAME_1),
        template = "my-template.json", region = "eastus2", group = "my-group"
)
```

`mytemplate.json` is located in resources. If you wish to share cloud resources among multiple test classes, set `perSuite` parameter to true, include such test classes in a JUnit5 suite and run the suite, not the test classes. Resources will be deleted once the suite is finished. If the parameter is set to false, resources are undeployed after the test class finishes.

About `AzureArmTemplate` parameters:
- `parameters` - define key-value parameters for Azure ARM template. String, integer, securestring and boolean types are supported. String values are converted into correct JSON structure for Azure ARM template.
- `template` - specify JSON template in resources
- `region` - define region where the template shall be deployed. If empty, value of `sunstone.azure.region` Sunstone Config property will be used
- `group` - define group where the template shall be deployed. If empty, value of `sunstone.azure.group` Sunstone Config property will be used
- `perSuite` - see above.

### Injection

Injection is based on a field's type and annotation. For injecting the hostname of a virtual machine:

```java
    @AzureVirtualMachine(name = "instanceName")
static Hostname hostname;
```

Following table shows what can be injected (type of the field) for what cloud resources (resource identification annotation)

| Type of the field        | annotation                                | note                                                   |
|--------------------------|-------------------------------------------|--------------------------------------------------------|
| `Hostname`               | `@AzureVirtualMachine`<hr/>`@AzureWebApp` | doesn't matter whether is standalone or domain <hr/> - |
| `OnlineManagementClient` | `@AzureVirtualMachine`                    | -                                                      |
| `AzureResourceManager`   | `@AzureAutoResolve`                       | group name doesn't matter                              |



### WildFly deployment

Deployment is based on method annotated by '@Deployment' annotation and resource identification annotation. For method requirements, see [README.md](README.md#wildfly-deployment). Following table shows a list of supported resources WildFly is running on for deploy operation.

| Resource          | annotation              | note |
|-------------------|-------------------------|------|
| Virtual machine   | `@AzureVirtualMachine`  | -    |
| Web application   | `@AzureWebApp`          | -    |

Example:
```java
@Deployment(name = "testapp.war")
@AzureVirtualMachine(name = "instanceName", group = "vmDeployGroup")
static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
        .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
}
```

The default mode is `Standalone`, and credentials come from Sunstone Config properties. You can also specify them in the annotation.

### About cloud resource identification annotations

###### AzureVirtualMachine
Annotation is used to identify Azure virtual machine. It may be expected, WildFly is running on it (for deployment operation or for injecting `OnlineManagementClient`):
- for injection support see [injection](Azure-README.md#injection) chapter
- for deploy operation support see [deploy](Azure-README.md#wildfly-deployment) chapter

If WildFly is running in standalone mode:
```java
@AzureVirtualMachine(
        name = "instanceName",
        group = "group",
        mode = OperatingMode.STANDALONE,
        standalone = @StandaloneMode(
                user = "mgmtUser",
                password = "mgmtPassword",
                port = "mgmtPort"))
static OnlineManagementClient client;
```

If WildFly is running in domain mode:
```java
@AzureVirtualMachine(
        name = "instanceName",
        group = "group")
@WildFly(
        mode = OperatingMode.DOMAIN,
        domain = @DomainMode(
                user = "mgmtUser",
                password = "mgmtPassword",
                port = "mgmtPort",
                host = "mgmtHost",
                profile = "mgmtProfile"))
static OnlineManagementClient client;
```

All values may contain expressions (`${my.property}`):
- `name` - mandatory, name of the virtual machine
- `group` - optional, if empty, `sunstone.azure.group` Sunstone Config property will be used
- mode - optional, default to OperatingMode.STANDALONE
- standalone - optional
  - `user` - optional. Management user for WildFly. If empty, `sunstone.wildfly.mgmt.user` Sunstone Config property will be used
  - `password` - optional. Management password for WildFly. If empty, `sunstone.wildfly.mgmt.password` Sunstone Config property will be used
  - `port` - optional. Management password for WildFly. If empty, `sunstone.wildfly.mgmt.port` Sunstone Config property will be used
- domain - optional
  - `user, password, port` - same as in standalone mode
  - `host` - optional. Wildfly host controller. If empty, `sunstone.wildfly.mgmt.host` Sunstone Config property will be used
  - `profile` - optional. Profile for WildFly. If empty, `sunstone.wildfly.mgmt.profile` Sunstone Config property will be used

###### AzureWebApp
Annotation is used to identify Azure Web application. It may be expected, WildFly is running on it (for deployment operation):
- for injection support see [injection](Azure-README.md#injection) chapter
- for deploy operation support see [deploy](Azure-README.md#wildfly-deployment) chapter

If WildFly is running in standalone mode:

```java
@Deployment
@AzureWebApplication(name = instanceName, group = webAppDeployGroup)
static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
        .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
        }
```

All values may contain expressions (`${my.property}`):
- `name` - mandatory, name of the virtual machine
- `group` - optional, if empty, `sunstone.azure.group` Sunstone Config property will be used
-

###### AzureAutoResolve
Annotation is used to identify Azure objects and clients with auto-resolution.
- injection supported, see [injection](Azure-README.md#wildfly-deployment) chapter

`group` parameter is optional. If empty, `sunstone.azure.group` Sunstone Config property will be used
