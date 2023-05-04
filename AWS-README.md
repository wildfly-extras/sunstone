# Amazon Web Services

This guide explains how to develop tests running against AWS resources.

## Prerequisites

You need to have programmatic access to AWS, which mean you have to have access key and secret.

## Introduction

There are two modules:
- aws
- aws-wildfly

The first one provides product agnostic functionality such as deploying AWS CloudFormation template, injecting common objects and so on.
Aws-wildfly is responsible for WildFly specific functionality - injecting OnlineManagementClient, deploying WAR to running WildFly and so on.

## Begin

Add the following properties into `sunstone.properties` located in your resources folder:

```properties
sunstone.aws.accessKeyID=${aws.accessKeyID}
sunstone.aws.secretAccessKey=${aws.secretAccessKey}
sunstone.aws.region=${aws.region}
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

Mind that only access key and secret are mandatory. Everything else you can also set in the annotations.

Add the following dependency:

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-aws</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

or product specific (which also gives you sunstone-aws)

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-aws-wildfly</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

## Test development
This chapter provides a guide and explains the logic behind cloud deployment, setup task, injection, deploy operation.

Only CloudFormation templates are supported for deploying resources to AWS:

```java
(1)
@WithAwsCfTemplate(parameters = {@Parameter(k = "instanceName", v = Example.NAME)},
        template = "eap.json", region = "${sunstone.aws.region}", perSuite = true)
(2)
@Setup(Example.SetupTask.class)
public class Example {
  public static final String NAME = "eap";

  (3)
  @AwsEc2Instance(nameTag = NAME)
  static Hostname hostname;

  @Test
  public void test() {}

  static class SetupTask extends AbstractSetupTask {
    @AwsEc2Instance(nameTag = NAME)
    OnlineManagementClient client;

    @Override
    public void setup() throws Exception {}

    @Override
    public void teardown() throws Exception {}
  }
}
```

The flow is:
**(1)** - At first, cloud resources defined by AWS CloudFormation template are deployed. You can specify multiple templates. You can use
expression (`${my.property}`) in all parameters - they are resolved by SmallRye config.

Note `perSuite`. If you wish to share cloud resources among multiple test classes, set `perSuite` parameter to true, include such test classes in a JUnit5 suite and run the suite, not the test classes. Resources will be deleted once the suite is finished. If the parameter is set to false (default value), resources are undeployed after the test class finishes.

**(2)** - Then, Setup task is  run. As you can see, you can also inject into the class.

Note: test class fields are injected **after** a setup task is run. Example static fields are **null** when the SetupTask is run (and can't be used there).

**(3)** - After the SetupTask is done, fields are injected

## Injection

Injection is based on a field's type and annotation. aws-wildfly module extend aws module in injection capabilities.

### aws module
See what kind of AWS resources are supported, what and how you can inject.
###### EC2 instance (virtual machine)
You can inject:
- Hostname - public ip of VM, see  and [here](aws/src/test/java/aws/cloudformation/di/suitetests/AwsHostnameTests.java)
- Instance - AWS SDK object for resource manipulation, see [here](aws/src/test/java/aws/cloudformation/di/suitetests/AwsEC2InstanceTests.java)

###### AWS SDK clients
See [here](aws/src/test/java/aws/cloudformation/di/AwsClientsTests.java), you can inject:
- Ec2Client
- S3Client

### aws-wildfly module
When you depend on aws-wildfly, you can inject wildfly specific objects. Here you can see what you can inject additionally to aws module
###### Virtual machine
Inject:
- OnlineManagement Client, see [standalone here](aws-wildfly/src/test/java/aws/cloudformation/di/suitetests/AwsStandaloneManagementClientTests.java) and [domain here](aws-wildfly/src/test/java/aws/cloudformation/di/suitetests/AwsDomainManagementClientTests.java)

You can deploy archive to running EAP instance. See [here](aws-wildfly/src/test/java/aws/cloudformation/archiveDeploy/ec2/suitetests/AwsEc2DeployFirstTest.java)
