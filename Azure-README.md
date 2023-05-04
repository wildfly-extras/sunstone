# Azure

This guide explains how to develop tests running against Azure resources.

## Prerequisites

You need to have programmatic access to Azure, which means you have created a service principal.

## Introduction

There are two modules:
- azure
- azure-wildfly

The first one provides product agnostic functionality such as deploying azure template, injecting common objects and so on. 
Azure-wildfly is responsible for WildFly specific functionality - injecting OnlineManagementClient, deploying WAR to running WildFly and so on.

## Begin

Add the following properties into `sunstone.properties` located in your resources' folder:

```properties
sunstone.azure.subscriptionId=${azure.subscriptionId}
sunstone.azure.tenantId=${azure.tenantId}
sunstone.azure.applicationId=${azure.applicationId}
sunstone.azure.password=${azure.password}
sunstone.azure.region=${azure.region:eastus2}
sunstone.azure.group=${azure.group}
```

If you are using WildFly, you may need to add:

```properties
sunstone.wildfly.mgmt.port=9990
sunstone.wildfly.mgmt.user=${wildfly.admin}
sunstone.wildfly.mgmt.password=${wildfly.password}
sunstone.wildfly.mgmt.connection.timeout=120000

sunstone.wildfly.mgmt.host=master
sunstone.wildfly.mgmt.profile=default
```

Last two are for domain mode.

Mind that only subscription id, tentant id, application id and password are mandatory. Everything else you can also set in the annotations.


Add the following dependency:

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-azure</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

or product specific (which also gives you sunstone-azure)

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-azure-wildfly</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

## Test development

This chapter provides a guide and explains the logic behind cloud deployment, setup task, injection, deploy operation.

Only Azure ARM templates are supported for deploying resources to Azure:

```java
(1) 
@WithAzureArmTemplate(parameters = @Parameter(k = "serverName", v = PGSQL_NAME),
        template = "posgresql.json", region = "eastus2", group = "${azure.group}", perSuite=true)
@WithAzureArmTemplate(parameters = {@Parameter(k = "appName", v = APP_NAME)},
        template = "eapWebApp.json")
(2)
@Setup(Example.SetupTask.class)
public class Example {
  public static final String PGSQL_NAME = "DSAzureTest-pgsql";
  public static final String APP_NAME = "DSAzureTest-webapp";

  (3)
  @AzurePgSqlServer(name = PGSQL_NAME)
  static Server pgsql;

  @AzureWebApplication(name = APP_NAME)
  Hostname app;

  @Test
  public void test() {}

  static class SetupTask extends AbstractSetupTask {
    @AzureWebApplication(name = APP_NAME)
    WebApp app;

    @Override
    public void setup() throws Exception {}

    @Override
    public void teardown() throws Exception {}
  }
}
```

The flow is:
**(1)** - At first, cloud resources defined by Azure template are deployed. You can specify multiple templates. You can use
expression (`${my.property}`) in all parameters - they are resolved by SmallRye config.

Note `perSuite`. If you wish to share cloud resources among multiple test classes, set `perSuite` parameter to true, include such test classes in a JUnit5 suite and run the suite, not the test classes. Resources will be deleted once the suite is finished. If the parameter is set to false (default value), resources are undeployed after the test class finishes.

Note `parameters`. Sunstone form JSON objects from values (due to how Azure templates works). String, integer, [securestring](https://learn.microsoft.com/en-us/azure/azure-resource-manager/templates/data-types#secure-strings-and-objects) and boolean types are supported.

**(2)** - Then, Setup task is  run. As you can see, you can also inject into the class.

Note: test class fields are injected **after** a setup task is run. Example static fields are **null** when the SetupTask is run (and can't be used there).

**(3)** - After the SetupTask is done, fields are injected


## Injection

Injection is based on a field's type and annotation. Azure-wildfly module extend azure module in injection capabilities.

### azure module
See what kind of Azure resources are supported, what and how you can inject.
###### Virtual machine
See [here](azure/src/test/java/sunstone/azure/armTemplates/di/AzVmTests.java), you can inject:
- Hostname - public ip of VM
- VirtualMachine - Azure SDK object for resource manipulation

###### Web application
See [here](azure/src/test/java/sunstone/azure/armTemplates/di/AzureWebAppTests.java), you can inject:
- Hostname - default hostname
- WebApp - Azure SDK object for resource manipulation

###### PostgreSQL server
See [here](azure/src/test/java/sunstone/azure/armTemplates/di/AzPgSqlTests.java), you can inject:
- Hostname - FQ domain name
- Server - Azure SDK object for resource manupulation

###### Azure SDK clients
See [here](azure/src/test/java/sunstone/azure/armTemplates/di/AzClientsTests.java), you can inject:
- AzureResourceManager
- PostgreSqlManager

### azure-wildfly module
When you depend on azure-wildfly, you can inject wildfly specific objects. Here you can see what you can inject additionally to azure module
###### Virtual machine
Inject:
- OnlineManagement Client, see [standalone here](azure-wildfly/src/test/java/sunstone/azure/armTemplates/di/AzStandaloneManagementClientTests.java) and [domain here](azure-wildfly/src/test/java/sunstone/azure/armTemplates/di/AzDomainManagementClientTests.java)

You can deploy archive to running EAP instance. See [here](azure-wildfly/src/test/java/sunstone/azure/armTemplates/archiveDeploy/vm/suitetests/AzureVmDeployFirstTest.java)

###### Web application
You can deploy archive to running EAP instance. See [here](azure-wildfly/src/test/java/sunstone/azure/armTemplates/archiveDeploy/webapp/suitetests/AzureWebAppDeployFirstTest.java)

