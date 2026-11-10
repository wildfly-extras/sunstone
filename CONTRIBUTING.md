# How to contribute


## Legal

All contributions to this repository are licensed under the [Apache License](https://www.apache.org/licenses/LICENSE-2.0), version 2.0 or later, or, if another license is specified as governing the file or directory being modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

### Compliance with Laws and Regulations

All contributions must comply with applicable laws and regulations, including U.S. export control and sanctions restrictions.
For background, see the Linux Foundation’s guidance:
[Navigating Global Regulations and Open Source: US OFAC Sanctions](https://www.linuxfoundation.org/blog/navigating-global-regulations-and-open-source-us-ofac-sanctions).

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
