brooklyn-cloudfoundry
=====

This package supports the application deployments on CloudFoundry-based platforms by Brooklyn.

Packages:
===
* `brooklyn-cloudfoundry-launcher` contains a brooklyn-distribution that integrates the features of this project.
* `brooklyn-cloudfoundry-location` contains a location that integrates (cf-java-client)[https://github.com/cloudfoundry/cf-java-client] and allows to manage some platform services.
* `brooklyn-cloudfoundry-entity` defines a simple entity that allows to deploy an application in a CF platform.

Build and Run
===

The project has to be build using maven.

````
mvn clean install
````
Before running the project, `CloudFoundryLocation` a could be configured in `brooklyn.properties`:

`````
#CloudFoundry
brooklyn.location.named.cloudfoundry-instance = cloudfoundry
brooklyn.location.named.cloudfoundry-instance.user = your_user
brooklyn.location.named.cloudfoundry-instance.password = your_pass
brooklyn.location.named.cloudfoundry-instance.org = available_org
brooklyn.location.named.cloudfoundry-instance.endpoint = https://api.run.pivotal.io #pivotal
brooklyn.location.named.cloudfoundry-instance.space = development
`````

Then, you can run the project by using:
````
cd brooklyn-cloudfoundry-launcher/target/brooklyn-cloudfoundry-launcher-1.0-SNAPSHOT-launcher/
nohup ./brooklyn launch
````

Deploying a first example
====
You can find the Brooklyn web console in http://localhost:8081. This Brooklyn instance contians the CloudFoundry experimental approach. When, Brooklyn will be running, you will be able to deploy your first CloudFoundry application example using the following Blueprint.
```
name: simple-appserver
services:
- type: org.apache.brooklyn.cloudfoundry.entity.webapp.CloudFoundryWebApp
  location: cloudfoundry-instance
  name: app
  brooklyn.config:
    artifact.url: https://github.com/kiuby88/brooklyn-library/releases/download/hello-world-sql-paas/brooklyn-example-hello-world-sql-webapp-in-paas.war
    application.name: first-application
  ```
