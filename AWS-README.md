# Amazon Web Services

This guide explains how to develop tests running against AWS resources.

## Prerequisites

You need to have programmatic access to AWS, which mean you have to have access key and secret.

## Begin

Add the following properties into `sunstone.properties` located in your resources folder:

```properties
sunstone.aws.accessKeyID=${aws.accessKeyID}
sunstone.aws.secretAccessKey=${aws.secretAccessKey}
sunstone.aws.region=${aws.region}

sunstone.wildfly.mgmt.port=9990
sunstone.wildfly.mgmt.user=${wildfly.admin}
sunstone.wildfly.mgmt.password=${wildfly.password}
sunstone.wildfly.mgmt.connection.timeout=120000
```

Mind that only access key and secret are mandatory. Everything else you can also set in the annotations.

Add the following dependency:

```xml
<dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-aws</artifactId>
    <version>${sunstone.version}</version>
</dependency>
```

## Test development

This chapter provides a guide and explains the logic behind the cloud deployment, injection and the WildFly deploy operation. See JavaDoc for relevant annotations.

### Cloud deployment

Only CloudFormation templates are supported for deploying resources to AWS:

```java
@WithAwsCfTemplate(
        parameters = @Parameter(k = "my-tag", v = "value-for-tag"),
        template = "mytemplate.yaml"
)
public class SingleAwsCfTemplateTest
```

`mytemplate.yaml` is located in resources. If you wish to share cloud resources among multiple test classes, set `perSuite` parameter to true, include such test classes in a JUnit5 suite and run the suite, not the test classes. Resources will be deleted once the suite is finished. If the parameter is set to false, resources are undeployed after the test class finishes.

About `WithAwsCfTemplate` parameters:
- `parameters` - define key-value parameters for CloudFormation template
- `template` - specify YAML template in resources
- `region` - define region where the template shall be deployed. If empty, value of `sunstone.aws.region` Sunstone Config property will be used
- `perSuite` - see above.

### Injection

Injection is based on a field's type and annotation. For injecting the hostname of an EC2 instance:
:

```java
@AwsEc2Instance(nameTag = "instanceName")
static Hostname hostname;
```

Following table shows what can be injected (type of the field) for what cloud resources (resource identification annotation)

| Type of the field        | Annotation        | note                                           |
|--------------------------|-------------------|------------------------------------------------|
| `Hostname`               | `@AwsEc2Instance` | doesn't matter whether is standalone or domain |
| `OnlineManagementClient` | `@AwsEc2Instance` | -                                              |
| `S3Client`               | `@AwsAutoResolve` | -                                              |
| `Ec2Client`              | `@AwsAutoResolve` | -                                              |


### WildFly deployment

Deployment is based on method annotated by '@Deployment' annotation and resource identification annotation. For method requirements, see [README.md](README.md#wildfly-deployment). Following table shows a list of supported resources WildFly is running on for deploy operation.

| Resource                 | annotation        | note |
|--------------------------|-------------------|------|
| EC2 Instance             | `@AwsEc2Instance` | -    |

Example:
```java
@Deployment(name = "testapp.war")
@AwsEc2Instance(nameTag = AwsTestConstants.instanceName, region = region)
static WebArchive deploy() {
    return ShrinkWrap.create(WebArchive.class)
            .addAsWebResource(new StringAsset("Hello World"), "index.jsp");
}
```

The default mode is `Standalone`, and credentials come from Sunstone Config properties. You can also specify them in the annotation.

### About cloud resource identification annotations

###### AwsEc2Instance
Annotation is used to identify AWS EC2 instance. It may be expected, WildFly is running on it (for deployment operation or for injecting `OnlineManagementClient`):
- for injection support see [injection](AWS-README.md#wildfly-deployment) chapter
- for deploy operation support see [deploy](AWS-README.md#injection) chapter

If WildFly is running in standalone mode:
```java
@AwsEc2Instance(
        nameTag = "instanceName",
        region = "region",
        mode = OperatingMode.STANDALONE,
        standalone = @StandaloneMode(
                user = "mgmtUser",
                password = "mgmtPassword",
                port = "mgmtPort"))
static OnlineManagementClient client;
```

All values may contain expressions (`${my.property}`):
- `nameTag` - mandatory, value of `name` tag of EC2 Instance
- `region` - optional, if empty, `sunstone.aws.region` Sunstone Config property will be used
- mode - optional, default to OperatingMode.STANDALONE
- standalone - optional
  - `user` - optional. Management user for WildFly. If empty, `sunstone.wildfly.mgmt.user` Sunstone Config property will be used
  - `password` - optional. Management password for WildFly. If empty, `sunstone.wildfly.mgmt.password` Sunstone Config property will be used
  - `port` - optional. Management password for WildFly. If empty, `sunstone.wildfly.mgmt.port` Sunstone Config property will be used
- domain - optional
  - `user, password, port` - same as in standalone mode
  - `host` - optional. Wildfly host controller. If empty, `sunstone.wildfly.mgmt.host` Sunstone Config property will be used
  - `profile` - optional. Profile for WildFly. If empty, `sunstone.wildfly.mgmt.profile` Sunstone Config property will be used


If WildFly is running in domain mode:
```java
@AwsEc2Instance(
        nameTag = "instanceName",
        region = "region",
        mode = OperatingMode.STANDALONE,
        standalone = @StandaloneMode(
                user = "mgmtUser",
                password = "mgmtPassword",
                port = "mgmtPort",
                host = "mgmtHost",
                profile = "mgmtProfile"))
static OnlineManagementClient client;
```

###### AwsAutoResolve
Annotation is used to identify AWS objects and clients with auto-resolution.
- injection supported, see [injection](AWS-README.md#wildfly-deployment) chapter

`region` parameter is optional. If empty, `sunstone.aws.region` Sunstone Config property will be used 
