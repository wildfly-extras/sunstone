# Release Procedure

This text assumes that the version number for a release is X.Y.Z.

1. Make new internal JClouds release
   if needed and update the `version.jclouds` property in `pom.xml`
1. Make sure `CHANGELOG.md` is up to date. Cross-check with GitHub Issues
   for given milestone
1. If needed: `git commit -m "add missing changelog entries"`
1. Reorder the entries in `CHANGELOG.md` from most important to least
   Up to this point, entries are typically added chronologically
   This is not the best order for release
1. Update the heading of the latest changelog section.
   It should look like `X.Y.Z (YYYY-MM-DD)` instead of
   `X.Y.Z (not yet released)`.
1. Update version in the `pom.xml`  
   `mvn versions:set -DgenerateBackupPoms=false -DnewVersion=X.Y.Z`
1. `git commit -a -m "version X.Y.Z"`
1. Your workspace should now be pristine. Check with `git status`
1. Run all the tests if possible (see below)  
1. Build  
   `mvn clean source:jar install`
1. Deploy  
   `mvn deploy -DskipTests`
1. `git tag sunstone-X.Y.Z"`
1. Close the relevant GitHub Milestone. Optionally create a new Milestone
1. Set a new `-SNAPSHOT` version in `pom.xml` and corresponding heading to the top of `CHANGELOG.md`.
   Its text should be: `X.Y+1.0 (not yet released)`.  
   `mvn versions:set -DgenerateBackupPoms=false -DnewVersion=X.Y+1.0-SNAPSHOT`
1. `git commit -a -m "next is X.Y+1.0"`
1. Upgrade the version in the dependant project (testsuites, quickstarts, ...) if possible
1. Send a release announcement with information about migration to new version

## Run the tests

```
EC2_ACCESS_KEY=...
EC2_SECRET_KEY=...
EC2_PRIVATE_KEY_FILE=${HOME}/.ssh/ec2.privatekey

OS_ENDPOINT=http://openstack-test.my-company.example:5000/v2.0
OS_PRIVATE_KEY_FILE=${HOME}/.ssh/openstack.privatekey
OS_USERNAME=tenant:tenant
OS_PASSWORD=...
OS_IMAGE=rhel-7.2-server-x86_64-cloud-released

AZURE_SUBSCRIPTION_ID=...
AZURE_PRIVATE_KEY_FILE=${HOME}/azure.pfx
AZURE_PRIVATE_KEY_PASSWORD=...
AZURE_STORAGE=...


mvn clean install -fae \
  -Dec2.accessKeyID=$EC2_ACCESS_KEY \
  -Dec2.secretAccessKey=$EC2_SECRET_KEY \
  -Dec2.keyPair=$USER \
  -Dec2.ssh.privateKeyFile=$EC2_PRIVATE_KEY_FILE \
  -Dazure.subscriptionId=$AZURE_SUBSCRIPTION_ID \
  -Dazure.privateKeyFile=$AZURE_PRIVATE_KEY_FILE \
  -Dazure.privateKeyPassword=$AZURE_PRIVATE_KEY_PASSWORD \
  -Dazure.image=b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-14_04_3-LTS-amd64-server-20160114.5-en-us-30GB \
  -Dazure.storage=$AZURE_STORAGE \
  -Dazure.ssh.user=jboss \
  -Dazure.ssh.password=very.123-complicated \
  -Dopenstack.endpoint=$OS_ENDPOINT \
  "-Dopenstack.username=$OS_USERNAME" \
  "-Dopenstack.password=$OS_PASSWORD" \
  "-Dopenstack.image=$OS_IMAGE" \
  "-Dopenstack.image.id=$OS_IMAGE_ID" \
  -Dopenstack.ssh.user=cloud-user \
  -Dopenstack.ssh.privateKeyFile=$OS_PRIVATE_KEY_FILE  \
  -Dopenstack.keypair=cloudsts
```
