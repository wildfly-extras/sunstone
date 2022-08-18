# JUni5 extension

Sunstone library comes with JUnit5 Sunstone extension for simpler testing in cloud environments.

The extension is able to control Cloud resources on test class level. The module allows you to deploy resources in several ways: 
- Azure template
- AWS CloudFormation template
- (TODO) JCloud resources providing property file depending on cloud:
  - [azure-arm-README](azure-arm-README.md) 
  - [baremetal-README](baremetal-README.md) 
  - [ec2-README](ec2-README.md) 
  - [docker-README](docker-README.md) 
  - [openstack-README](openstack-README.md) 
  - [wildfly-README](wildfly-README.md)

## Test development

This section shows a simple way how use Sunstone extension do deploy to Cloud using various technologies.

### Add sunstone.properties
```properties
junit5.az.subscriptionId=${azure.subscriptionId:subscribtionID}
junit5.az.tenantId=${azure.tenantId:tenantID}
junit5.az.applicationId=${azure.applicationId:appID}
junit5.az.password=${azure.password:pass}
junit5.az.region=${azure.region:eastus2}
junit5.az.group=${azure.group:group}

junit5.aws.accessKeyID=${ec2.accessKeyID}
junit5.aws.secretAccessKey=${ec2.secretAccessKey}
junit5.aws.region=${ec2.region:us-east-1}

```

### Add maven dependencies

```xml
<dependencies>
  <dependency>
    <groupId>org.wildfly.extras.sunstone</groupId>
    <artifactId>sunstone-junit5</artifactId>
    <version>${version.org.wildfly.extras.sunstone}</version>
  </dependency>
</dependencies>
```
WildFly client libraries must also be specified as described
in [Creaper library README](https://github.com/wildfly-extras/creaper/blob/master/README.md#jboss-as-7--wildfly-client-libraries).

### Use Sunstone extension
To enable the extension, simply add annotation on a test class:
```Java
@Sunstone
public class Test {}
```



### Describe cloud deployment
You can describe resources to be deployed using annotations `@With*` on a test class. See following examples for Azure/AWS specific resources or JClouds that provide abstraction for several Cloud resources.

#### Azure templates

```java
@Sunstone
@WithAzureArmTemplate("appservices/clusteredWebApp.json")
public class AzureTest {}
```

In this case, template is located in resources. You can also provide a raw content or URL and the template shall be downloaded. Resources are deployed to the group and region defined in `sunstone.properties`.

You can download JSON definition of already existing resources or downloading JSON for automation in create form.

Azure template allows you to deploy and configure Azure specific resources using native technologies.

#### AWS CloudFormation template

```java
@Sunstone
@WithAwsCfTemplate("aws/messaging-mdb.yaml")
public class AWSTest {}
```

In this case, template is located in resources. You can also provide a raw content or URL and the template shall be downloaded. Resources are deployed to a stack of random name in region defined in `sunstone.properties`

You can use Former2 tool to obtain YAML definition of already existing resources.

CloudFormation template allows you to deploy and configure AWS specific resources using native technologies.

#### JClouds
TODO

```java
@Sunstone
@WithJClouds("test.properties")
public class JCloudTest {}
```

In this case, property file is located in resources. You can also provide a raw content or URL and the file shall be downloaded.

Contract for different providers can be found at:
- [azure-arm-README](azure-arm-README.md)
- [baremetal-README](baremetal-README.md)
- [ec2-README](ec2-README.md)
- [docker-README](docker-README.md)
- [openstack-README](openstack-README.md)
- [wildfly-README](wildfly-README.md)

JCloud enables you to share a test among different providers.

### Provide additional configuration
Sunstone allows you to define a setup task with enabled DI. Add the `@Setup` annotation on test class

```java
@Sunstone
@WithAwsCfTemplate("aws/messaging-mdb.yaml")
@Setup(Configure2LCApp.class)
public class AWSTest {}
```
```java

public class Configure2LCApp extends AbstractSetupTask {
    
  @SunstoneResource(of = "eapqe-instance", hint = AWS_EC2_INSTANCE)
  OnlineManagementClient client;

  @Override
  public void setup() throws Exception {
    // configure eap
  }

  @Override
  public void cleanup() throws Exception {
    // clean the configuration
  }
}
```


### Use DI for test development
Sunstone allows you to inject several resources: EAP management client, cloud clients, ... Fields may be either static (injected once) or non-static (injected per test case).

```java
@Sunstone
@WithAwsCfTemplate("aws/messaging-mdb.yaml")
@Setup(Configure2LCApp.class)
public class AWSTest {
  @SunstoneResource(of = "eapqe-mdb", hint = AWS_EC2_INSTANCE)
  static Hostname mdbInstanceHostname;
}
```

### Develop tests
```java
@Sunstone
@WithAwsCfTemplate("aws/messaging-mdb.yaml")
@Setup(Configure2LCApp.class)
public class AWSTest {
  @SunstoneResource(of = "eapqe-mdb", hint = AWS_EC2_INSTANCE)
  static Hostname mdbInstanceHostname;
  
  @Test
  public void test() {
      assert Rest.get(mdbInstanceHostname.get()).code() == 200;
  }
}
```

Users usually have a configuration file `arquillian.xml` on the classpath and its content looks similar to this one:

```xml
<arquillian xmlns="http://jboss.org/schema/arquillian" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">
    <container qualifier="jboss" default="true">
        <configuration>
            <!-- ... properties -->
        </configuration>
    </container>
</arquillian>
```

Let's move the testing into cloud. We'll use Docker instead of real Cloud provider for this simple scenario.

### Add maven dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.wildfly.extras.sunstone</groupId>
        <artifactId>sunstone-junit5</artifactId>
        <version>${version.org.wildfly.extras.sunstone}</version>
    </dependency>
</dependencies>
```
