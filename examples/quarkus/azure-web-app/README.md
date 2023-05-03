# quarkusQS

This module provides quarkus example for azure deployment and testing.
This application provides single endpoint `/hello` that returns "Hello World" message.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
mvn compile quarkus:dev
``` 

## Packaging and running the application

The application can be packaged using:
```shell script
mvn package
```
It produces the `quarkus-run.jar` file in the `target` directory.
Application is packaged as _über-jar_ according to `application.properties` file.

The application, packaged as a _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

# Azure deployment
Application uses `azure-maven-plugin` to deploy application to azure, with command:

```shell script
mvn verify \
  -Dazure.subscriptionId=$AZURE_SUBSCRIPTION \
  -Dazure.tenantId=$AZURE_TENANT \
  -Dazure.clientId=$AZURE_SERVICE_PRINCIPAL_ID \
  -Dazure.password=$AZURE_SERVICE_PRINCIPAL_PASSWORD \
  -Dazure.group=$AZURE_GROUP \
  -Dazure.appName=$AZURE_APP_NAME
```

With `azure.group` and `azure.appName` parameters being optional.

These parameters are also supplied to `sunstone.properties` file and used via `QuarkusTestConstants.java`.

Currently, there is no option to undeploy application from azure, so it is necessary to do it manually.

```shell script
az group delete --name $AZURE_GROUP
```

## Testing on Azure
Due to plugin deployment needing prepared _über-jar_, tests have to be run after `package` phase.
Thus, deployment is done in `pre-integration-test` phase, and tests are run in `integration-test` phase via `failsafe` plugin.
