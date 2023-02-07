# How to contribute


## Build and run tests

The library is a maven project, so you can simply use `mvn install` command to build it.
Nevertheless the tests requires Azure service principal and AWS programatic access.

```bash
mvn clean install -DtestLogToFile=false \
  -Dazure.subscriptionId=$AZURE_SUBSCRIPTION_ID \
  -Dazure.tenantId=$AZURE_TENANT_ID \
  -Dazure.applicationId=$AZURE_APPLICATION_ID \
  -Dazure.password=$AZURE_PASSWORD \
  -Daws.accessKeyID=$AWS_ACCESS_KEY_ID \
  -Daws.secretAccessKey=$AWS_SECRET \
```
