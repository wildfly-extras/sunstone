# Sunstone

Simple library which helps to control virtual machines in cloud environments.
It's aimed mainly to testing WildFly application server.

> Name: [Sunstone](https://en.wikipedia.org/wiki/Sunstone_\(medieval\))
> is a crystal that was supposedly used by Vikings to navigate in cloudy weather.


## Motivation

### Why?

*Why yet another library?*  
*Because clouds exist. And they are many! Moreover, everybody loves new libraries, of course.*

The library is here to simplify test development for new clouds.

### What?

*What does it bring to me?*  
*One view to rule them all.*

* United approach for all supported cloud providers
* Deploy resources to 3rd party cloud with Azure ARM templates, AWS CloudFormation Templates, ... 
* Inject SDK clients, Management clients, hostnames of created resources, ...
* Possibility to control WildFly nodes with [Creaper](https://github.com/wildfly-extras/creaper)
* JUnit5 test framework support

### How?

Depends on the cloud you are working with. For every supported cloud, there is a module that brings all API and support you need. The flow always follow (even if you combine two clouds in one single test):

1. Cloud deployment - creating cloud resources  
2. Setup task 
3. WildFly deployment
4. Inject static test class fields
5. Inject non-static test instance fields

For more information about specific clouds, see:
* [Azure README](Azure-README.md)
* [AWS README](AWS-README.md)

##### Cloud deployment
Sunstone deploys and manages lifecycle of cloud resources - deploys resources before tests and deletes them once tests are finished. Various deploy methods are supported.

See:
* [Azure cloud deploy](Azure-README.md#cloud-deployment)
* [AWS cloud deploy](AWS-README.md#cloud-deployment)


##### Setup task

'@Setup' annotation on a test class defines setup tasks that will be run before the first WildFly deployment. `teardown` method will be run after the last WildFly deployment is undeployed. Setup task class must extend `AbstractSetupTask` class. You can also inject static and non-static field that follows same principles as described in [injection subchapter](README.md#injection)

```java
@Setup(SetupTest.StaticClassTask.class)
public class SetupTest {

    @Test
    public void test() {
        // test logic
    }

    static class StaticClassTask extends AbstractSetupTask {
        @Override
        public void setup() throws Exception {
            // configure WildFly or cloud resources
        }

        @Override
        public void teardown() throws Exception {
            // tear down the configuration
        }
    }
}
```

##### WildFly deployment

WildFly deploy operation is specific to the type of resources WildFly is running on. The archive that you want to be deployed, you must use `@Deployment` annotation on a static method returning the archive. Supported return types:
- File (local)
- Path (local)
- Archive (Shrinkwrap)
- InputStream

The method must be annotated by cloud-specific annotation that identifies the cloud resource. The annotations come from cloud specific Sunstone modules. 

See:
* [Azure WildFly deploy](Azure-README.md#wildfly-deployment)
* [AWS WildFly deploy](AWS-README.md#wildfly-deployment)

##### Injection

Sunstone supports injection of several resources, i.e. hostname of resources (Azure Web app, AWS EC2 instance, ... ). The logic is that if you want to inject a particular field od particular type, you need to identify the cloud resource. I.e. you want to inject a hostname. The type must be `Hostname`. You also need to annotate the field with cloud-specific annotation that identify such resources. The annotations come from cloud specific Sunstone modules.

See:
* [Azure inject](Azure-README.md#injection)
* [AWS inject](AWS-README.md#injection)

## Logging

SLF4J is used as a logging facade, so you have to have the appropriate adapter on the classpath. If you use Logback,
you don't have to do anything. For other loggers, see [the SLF4J manual](http://www.slf4j.org/manual.html).

The loggers are called `sunstone.*`, short and clear. (For example: `sunstone.core`)

## License

* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
