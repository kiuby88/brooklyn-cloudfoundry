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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.location.MachineLocation;
import org.apache.brooklyn.api.location.MachineProvisioningLocation;
import org.apache.brooklyn.api.location.NoMachinesAvailableException;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.core.entity.BrooklynConfigKeys;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.core.location.LocationConfigKeys;
import org.apache.brooklyn.core.location.cloud.CloudLocationConfig;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.core.config.ResolvingConfigBag;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class CloudFoundryLocation extends AbstractLocation implements MachineProvisioningLocation<MachineLocation>, CloudFoundryLocationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryLocation.class);

    private CloudFoundryOperations cloudFoundryOperations;
    private CloudFoundryClient cloudFoundryClient;

    public CloudFoundryLocation() {
        super();
    }

    public CloudFoundryLocation(Map<?, ?> properties) {
        super(properties);
    }

    @Override
    public void init() {
        super.init();
    }

    protected CloudFoundryClient getCloudFoundryClient() {
        return getCloudFoundryClient(MutableMap.of());
    }

    protected CloudFoundryClient getCloudFoundryClient(Map<?, ?> flags) {
        ConfigBag conf = (flags == null || flags.isEmpty())
                ? config().getBag()
                : ConfigBag.newInstanceExtending(config().getBag(), flags);
        return getCloudFoundryClient(conf);
    }

    protected CloudFoundryClient getCloudFoundryClient(ConfigBag config) {
        if (cloudFoundryClient == null) {
            CloudFoundryClientRegistry registry = getConfig(CF_CLIENT_REGISTRY);
            cloudFoundryClient = registry.getCloudFoundryClient(
                    ResolvingConfigBag.newInstanceExtending(getManagementContext(), config), true);
        }
        return cloudFoundryClient;
    }

    protected CloudFoundryOperations getCloudFoundryOperations() {
        return getCloudFoundryOperations(MutableMap.of());
    }

    protected CloudFoundryOperations getCloudFoundryOperations(Map<?, ?> flags) {
        ConfigBag conf = (flags == null || flags.isEmpty())
                ? config().getBag()
                : ConfigBag.newInstanceExtending(config().getBag(), flags);
        return getCloudFoundryOperations(conf);
    }

    protected CloudFoundryOperations getCloudFoundryOperations(ConfigBag config) {
        if (cloudFoundryOperations == null) {
            CloudFoundryClientRegistry registry = getConfig(CF_CLIENT_REGISTRY);
            cloudFoundryOperations = registry.getCloudFoundryOperations(
                    ResolvingConfigBag.newInstanceExtending(getManagementContext(), config), true);
        }
        return cloudFoundryOperations;
    }

    @Override
    public MachineLocation obtain(Map<?, ?> flags) throws NoMachinesAvailableException {
        ConfigBag setupRaw = ConfigBag.newInstanceExtending(config().getBag(), flags);
        ConfigBag setup = ResolvingConfigBag.newInstanceExtending(getManagementContext(), setupRaw);

        cloudFoundryClient = getCloudFoundryClient(setup);
        return createCloudFoundryContainerLocation(setup);
    }

    private MachineLocation createCloudFoundryContainerLocation(ConfigBag setup) {
        // Lookup entity flags
        Object callerContext = setup.get(LocationConfigKeys.CALLER_CONTEXT);
        if (callerContext == null || !(callerContext instanceof Entity)) {
            throw new IllegalStateException("Invalid caller context: " + callerContext);
        }
        Entity entity = (Entity) callerContext;

        String applicationName = entity.config().get(VanillaCloudFoundryApplication.APPLICATION_NAME);
        String host = entity.config().get(VanillaCloudFoundryApplication.APPLICATION_HOST);
        String domainName = entity.config().get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        int memory = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_MEMORY);
        int disk = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_DISK);
        int instances = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_INSTANCES);
        String artifact = entity.config().get(VanillaCloudFoundryApplication.ARTIFACT_PATH);
        Path artifactLocalPath;
        try {
            artifactLocalPath = new UrlResource(artifact).getFile().toPath();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        String buildpack = entity.config().get(VanillaCloudFoundryApplication.BUILDPACK);

        // TODO createPushApplicationRequest
        PushApplicationRequest.Builder pushApplicationRequestBuilder = PushApplicationRequest.builder();
        if (!isVanillaCloudFoundryApplication(entity))
            throw new IllegalStateException("Can't deploy entity type different than " + VanillaCloudFoundryApplication.class.getSimpleName());

        pushApplicationRequestBuilder
                .name(applicationName)
                .healthCheckType(ApplicationHealthCheck.NONE) // TODO is it needed?
                .randomRoute(true)
                .buildpack(buildpack)
                .application(artifactLocalPath)
                //.domain(domainName)
                .diskQuota(disk)
                .memory(memory);

        // this is pushApplication
        getCloudFoundryOperations().applications().push(pushApplicationRequestBuilder.build()).block();

        ApplicationDetail applicationDetail = getCloudFoundryOperations().applications()
                .get(GetApplicationRequest.builder()
                        .name(applicationName)
                        .build())
                .block();

        // see https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html#other-ssh-access
        GetInfoResponse info = getCloudFoundryClient().info().get(GetInfoRequest.builder().build()).block();
        String sshEndpoint = info.getApplicationSshEndpoint();
        String sshCode = getCloudFoundryOperations().advanced().sshCode().block();
        String address = Iterables.getOnlyElement(applicationDetail.getUrls());
        Integer port = Integer.parseInt(Iterables.get(Splitter.on(":").split(sshEndpoint), 1));
        LocationSpec<SshMachineLocation> locationSpec = LocationSpec.create(SshMachineLocation.class)
                .configure("address", address)
                .configure(CloudFoundryLocationConfig.APPLICATION_NAME, applicationName)
                .configure(SshMachineLocation.PRIVATE_ADDRESSES, ImmutableList.of(address))
                .configure(CloudLocationConfig.USER, String.format("cf:%s/0", applicationDetail.getId()))
                .configure(SshMachineLocation.PASSWORD, sshCode)
                .configure(SshMachineLocation.SSH_PORT, port)
                .configure(BrooklynConfigKeys.SKIP_ON_BOX_BASE_DIR_RESOLUTION, true)
                .configure(BrooklynConfigKeys.ONBOX_BASE_DIR, "/tmp")
                .configure(CALLER_CONTEXT, setup.get(CALLER_CONTEXT));

        return getManagementContext().getLocationManager().createLocation(locationSpec);
    }

    protected boolean isVanillaCloudFoundryApplication(Entity entity) {
        return entity.getEntityType().getName().equalsIgnoreCase(VanillaCloudFoundryApplication.class.getName());
    }

    @Override
    public MachineProvisioningLocation<MachineLocation> newSubLocation(Map<?, ?> map) {
        return null;
    }

    @Override
    public void release(MachineLocation machine) {
        getCloudFoundryOperations().applications().delete(DeleteApplicationRequest.builder()
                .name(machine.config().get(CloudFoundryLocationConfig.APPLICATION_NAME))
                .deleteRoutes(true)
                .build()
        ).block();
    }

    @Override
    public Map<String, Object> getProvisioningFlags(Collection<String> collection) {
        return null;
    }

}
