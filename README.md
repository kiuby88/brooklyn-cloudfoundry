brooklyn-cloudfoundry
=====

 [![Build Status](https://travis-ci.org/kiuby88/brooklyn-cloudfoundry.svg?branch=master)](https://travis-ci.org/kiuby88/brooklyn-cloudfoundry)

This package supports the application deployments on CloudFoundry-based platforms by Brooklyn. It was developed as part of Google Summer of Code 2016 program, under the [Adding PaaS support to Brooklyn proposal](https://summerofcode.withgoogle.com/projects/#6531111119224832)

##Description
This project contains a Brooklyn plugin which allows Cloud Foundry-based platforms management to be added to Brooklyn, such as [Pivotal Web Services](https://run.pivotal.io/) or [IBM Bluemix](https://console.ng.bluemix.net/).

##Usage
This project provides different elements to manage the Cloud Foundry services.

Building using maven
```
mvn clean install
```
Adding generated jar, `target/brooklyn-cloudfoundry-1.0-SNAPSHOT.jar`, to Brooklyn [dropins folder](https://brooklyn.apache.org/v/0.9.0/ops/externalized-configuration.html#writing-custom-external-configuration-suppliers).

###Location
Cloud Foundry platforms can be described using an `Location`, concretely a `CloudFoundryPaasLocation` was developed in order to represent an Cloud Foundry instance. Here’s a very simple YAML blueprint plan, to explain the `CloudFoundryPaasLocation` configuration:
````
name: CloudFoundryPaasLocation configuration example
location:
  cloudfoundry:
    identity: <user@mail.com>
    credential: <password>
    endpoint: <target cloud foundry instance api>
    org: <target user organization>
    space: <target space inside of the organization>
````
You’ll need to replace the `identity` and `credential` with the `user name` and `user password` in the target platform.
`endpoint` has to be configured using the api endpoint of the platform, for example Pivotal Web Services uses the API address `api.run.pivotal.io`. `org` and `space` will depend on the user profile in the target platform.

`CloudFoundryPaasLocation` provides an interface to manage the Cloud Foundry REST API, which is useful to develop entities to manage the Cloud Foundry resources, as Applications.

### Entities. CloudFoundry Applications and Services (addons)
This project defines some entities to manage the Cloud Foundry Application and services.
Currently, a `VanillaCloudFoundryApplication` entity allows generic applications to be deployed and managed inside of the target plaform. Services (addons) can also be managed through an entity, `CloudFoundryServices`. Following, you can see an YAML blueprint example of an application with a database:
````
name: Vanilla CloudFoundry example
location: ...
services:
- type: org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication
  id: vanilla-app
  brooklyn.config:
    path: https://github.com/kiuby88/brooklyn-cloudfoundry/blob/feature/binding-string-entity/src/test/resources/brooklyn-example-hello-world-sql-webapp-in-paas.war?raw=true
    buildpack: https://github.com/cloudfoundry/java-buildpack.git
    services:
    - $brooklyn:component("my-service").attributeWhenReady("cloudFoundry.service.instance.name")
    env:
      brooklyn.example.db.url: $brooklyn:component("my-service").attributeWhenReady("service.mysql.jdbc")

- type: org.apache.brooklyn.cloudfoundry.entity.service.mysql.CloudFoundryMySqlService
  id: my-service
  brooklyn.config:
    serviceName: cleardb
    plan: spark
    creationScriptTemplateUrl: https://raw.githubusercontent.com/kiuby88/brooklyn-cloudfoundry/feature/binding-string-entity/src/test/resources/chat-database.sql
`````
`VanillaCloudFoundryApplication` allows to deploy a generic application. `buildpack` property specify the required technology package that has to be installed in the instances to run the application. You can find [here](https://docs.cloudfoundry.org/buildpacks/) the official suported buildpacks. 

Furthermore, `VanillaCloudFoundryApplication` allows to define the required resources to run the application by means of `memory`, `disk_quota` and `instances` properties ([here an example](https://github.com/kiuby88/brooklyn-cloudfoundry/blob/master/src/test/resources/vanilla-cf-resources-profile.yml)). This entity allows manual scaling in runtime, so modify the memory, disk or used instancescan be modified by means of Brooklyn interface ([effectors](https://brooklyn.apache.org/v/latest/concepts/configuration-sensor-effectors.html#sensors-and-effectors)).

`path` represents the application artifact that will be updated to the platform.
`env` allows to define a map of environment variables that will be used by the deployed application.
`services` allows to specify the service instances that will be bound to the application.


`CloudFoundryMySqlService` represents a MySql-based Cloud Foundry service. The services requires an `serviceName`, which represents an available service in the target platform, a `plan`. Moreover, the application's and the service's lifecycle were integrated in order to allows the database to be initiated using the `creationScriptTemplateUrl` once the service is being created and bound to the application.

##TODO: 
Currently, we are figuring out about how we should integrate this project in [apache/brooklyn](https://github.com/apache/brooklyn/) repository.

