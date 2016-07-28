/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.cloudfoundry.location;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.brooklyn.core.entity.StartableApplication;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.core.config.ResolvingConfigBag;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

public class CloudFoundryPaasLocation extends AbstractLocation implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public CloudFoundryPaasLocation() {
        super();
    }

    public CloudFoundryPaasLocation(Map<?, ?> properties) {
        super(properties);
    }

    public CloudFoundryClient getClient() {
        return getClient(MutableMap.of());
    }
    public CloudFoundryClient getClient(Map<?,?> flags) {
        ConfigBag conf = (flags==null || flags.isEmpty())
                ? config().getBag()
                : ConfigBag.newInstanceExtending(config().getBag(), flags);
        return getClient(conf);
    }

    public CloudFoundryClient getClient(ConfigBag config) {
        CloudFoundryClientRegistry registry = getConfig(CF_CLIENT_REGISTRY);
        return registry.getCloudFoundryClient(ResolvingConfigBag.newInstanceExtending(getManagementContext(), config), true);
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }

    public void push() {

        DefaultCloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(getClient())
                //.dopplerClient(dopplerClient)
                //.uaaClient(uaaClient)
                .organization("example-organization")
                .space("example-space")
                .build();

        cloudFoundryOperations.applications()
                .push(PushApplicationRequest.builder()
                        .application(Paths.get("path"))
                        .healthCheckType(ApplicationHealthCheck.PORT)
                        .buildpack("staticfile_buildpack")
                        .diskQuota(512)
                        .memory(64)
                        .name("name")
                        .noStart(true)
                        .build());
    }

    public Iterable<OrganizationSummary> listOrganizations() {
        DefaultCloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(getClient())
                .organization("example-organization")
                .space("example-space")
                .build();

        return cloudFoundryOperations.organizations().list().toIterable();
    }

    public void startApplication(String applicationName) {
        DefaultCloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(getClient())
                .organization("example-organization")
                .space("example-space")
                .build();
        cloudFoundryOperations.applications().start(StartApplicationRequest.builder().name(applicationName).build()).block();
    }

    public void stop(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void restart(String applicationName) {
        getClient().restartApplication(applicationName);
    }

    public void delete(String applicationName) {
        getClient().deleteApplication(applicationName);
    }

    public void setEnv(String applicationName, Map<String, String> env) {
        getClient().setEnv(applicationName, env);
    }

    public Map<String, String> getEnv(String applicationName) {
        return getClient().getEnv(applicationName);
    }

    public void setInstancesNumber(String applicationName, int instancesNumber) {
        getClient().setInstancesNumber(applicationName, instancesNumber);
    }

    public void setDiskQuota(String applicationName, int diskQuota) {
        getClient().setDiskQuota(applicationName, diskQuota);
    }

    public void setMemory(String applicationName, int memory) {
        getClient().setMemory(applicationName, memory);
    }

    public int getInstancesNumber(String applicationName) {
        return getClient().getInstancesNumber(applicationName);
    }

    public int getDiskQuota(String applicationName) {
        return getClient().getDiskQuota(applicationName);
    }

    public int getMemory(String applicationName) {
        return getClient().getMemory(applicationName);
    }

}
