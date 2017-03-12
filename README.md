brooklyn-cloudfoundry
=====

 [![Build Status](https://travis-ci.org/kiuby88/brooklyn-cloudfoundry.svg?branch=master)](https://travis-ci.org/kiuby88/brooklyn-cloudfoundry)

This package supports the application deployments on CloudFoundry-based platforms by Brooklyn. It was developed as part of Google Summer of Code 2016 program, under the [Adding PaaS support to Brooklyn proposal](https://summerofcode.withgoogle.com/projects/#6531111119224832)

# Description
This project contains a Brooklyn plugin which allows Cloud Foundry-based platforms management to be added to Brooklyn, such as [Pivotal Web Services](https://run.pivotal.io/) or [IBM Bluemix](https://console.ng.bluemix.net/).

Cloud Foundry platforms can be described using an `Location`, concretely a `CloudFoundryLocation` was developed in order to represent an Cloud Foundry instance. Here’s a very simple YAML blueprint plan, to explain the `CloudFoundryLocation` configuration:
```
location:
  cloudfoundry:
    identity: <user@mail.com>
    credential: <password>
    endpoint: <target cloud foundry instance api>
    org: <target user organization>
    space: <target space inside of the organization>
```

You’ll need to replace the `identity` and `credential` with the `user name` and `user password` in the target platform.
`endpoint` has to be configured using the api endpoint of the platform, for example PCF Dev requires `api.local.pcfdev.io`.

`CloudFoundryLocation` provides an interface to manage the Cloud Foundry REST API, which is useful to develop entities to manage the Cloud Foundry resources, as Applications.

# Usage
This project provides different elements to manage the Cloud Foundry services.

Building using maven
```
mvn clean install
```

## Add Cloudfoundry location to classic Brooklyn

Add the generated [artifact](target/brooklyn-cloudfoundry-1.0-SNAPSHOT.jar) to Brooklyn [dropins folder](https://brooklyn.apache.org/v/0.9.0/ops/externalized-configuration.html#writing-custom-external-configuration-suppliers).

## Add Cloudfoundry location to Brooklyn Karaf

```
./bin/karaf

feature:repo-add file:/path/to/your/brooklyn-cloudfoundry/git/andreaturli/brooklyn-cloudfoundry/karaf/features/target/feature.xml
feature:install brooklyn-location-cloudfoundry
```
close the karaf shell and then launch AMP as usual
```
./bin/start
```

# Brookyln CloudFoundry blueprint examples

## VanillaCloudFoundryApplication with services

A `VanillaCloudFoundryApplication` entity allows generic applications to be deployed and managed inside of the target plaform. Services  can also be managed through an entity, `CloudFoundryServices`. Following, you can see an YAML blueprint example of an application with a database:
```
location: my-cloudfoundry
services:
- type: org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication
  brooklyn.config:
    nameApp: spring-music
    path: file:/Users/andrea/projects/cf/spring-music/build/libs/spring-music.jar
    buildpack: https://github.com/cloudfoundry/java-buildpack.git
    services:
    - p-mysql:
        plan: 512mb
        instanceName: mysql-for-spring-music
```
`VanillaCloudFoundryApplication` allows to deploy a generic application. `buildpack` property specify the required technology package that has to be installed in the instances to run the application. You can find [here](https://docs.cloudfoundry.org/buildpacks/) the official suported buildpacks.

Furthermore, `VanillaCloudFoundryApplication` allows to define the required resources to run the application by means of `memory`, `disk_quota` and `instances` properties ([here an example](https://github.com/kiuby88/brooklyn-cloudfoundry/blob/master/src/test/resources/vanilla-cf-resources-profile.yml)). This entity allows manual scaling in runtime, so modify the memory, disk or used instancescan be modified by means of Brooklyn interface ([effectors](https://brooklyn.apache.org/v/latest/concepts/configuration-sensor-effectors.html#sensors-and-effectors)).

`path` represents the application artifact that will be updated to the platform.
`env` allows to define a map of environment variables that will be used by the deployed application.
`services` allows to specify the service instances that will be bound to the application.

## CloudFoundryAppFromManifest 
Most likely as CF developer you have already defined some CF manifests. By using `CloudFoundryAppFromManifest` entity, you can execute that manifest from Apache Brooklyn

```
location: my-cloudfoundry
services:
- type: org.apache.brooklyn.cloudfoundry.entity.CloudFoundryAppFromManifestImpl
  brooklyn.config:
    cf.manifest.contents: |
      name: spring-music
      path: file:/Users/andrea/projects/cf/spring-music/build/libs/spring-music.jar
      memory: 512
      services:
      - mysql-for-spring-music
```

Notice, `services` have a different semantic compared to `VanillaCloudFoundryApplication`. There you can define the `service` you want to instantiate by specifying 
the `plan` and `instanceName` while here `services` refer to the `serviceInstanceNames` that needs to be created manually using 
`cf` cli (i.e.: `cf create-service p-mysql 512mb mysql-for-spring-music`) 

# TODO
Currently, we are figuring out about how we should integrate this project in [apache/brooklyn](https://github.com/apache/brooklyn/) repository.

