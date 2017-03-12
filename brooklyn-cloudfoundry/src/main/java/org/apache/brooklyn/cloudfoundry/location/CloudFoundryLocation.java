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

import static org.apache.brooklyn.cloudfoundry.entity.CloudFoundryAppFromManifest.CONFIGURATION_CONTENTS;
import static org.apache.brooklyn.cloudfoundry.entity.CloudFoundryAppFromManifest.CONFIGURATION_URL;
import static org.apache.brooklyn.util.text.Strings.isBlank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.location.MachineLocation;
import org.apache.brooklyn.api.location.MachineProvisioningLocation;
import org.apache.brooklyn.api.location.NoMachinesAvailableException;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryAppFromManifest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.core.entity.BrooklynConfigKeys;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.core.location.cloud.CloudLocationConfig;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.core.config.ResolvingConfigBag;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.yaml.Yamls;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
        Entity entity = lookUpEntityFromCallerContext(setup.get(CALLER_CONTEXT)) ;

        PushApplicationRequest pushApplicationRequest;
        List<String> serviceInstanceNames;
        if (isVanillaCloudFoundryApplication(entity)) {
            pushApplicationRequest = createPushApplicationRequestFromVanillaCloudFoundryApplication(entity);
            serviceInstanceNames = createInstanceServices(entity.config().get(VanillaCloudFoundryApplication.SERVICES));
        } else if(isCloudFoundryAppFromManifet(entity)) {
            Map<?, ?> manifestAsMap = getMapFromManifest(getManifestYamlFromEntity(entity));
            pushApplicationRequest = createPushApplicationRequestFromManifest(manifestAsMap);
            serviceInstanceNames = getServiceInstancesFromManifest(manifestAsMap);
        } else {
            throw new IllegalStateException("Can't deploy entity type different than " + VanillaCloudFoundryApplication.class.getSimpleName());
        }

        pushApplication(pushApplicationRequest);

        String applicationName = pushApplicationRequest.getName();

        // bind services
        if (!serviceInstanceNames.isEmpty()) {
            bindServices(applicationName, serviceInstanceNames);
            restartApplication(applicationName);
        }

        LocationSpec<SshMachineLocation> locationSpec = buildLocationSpec(applicationName, setup.get(CALLER_CONTEXT));
        return getManagementContext().getLocationManager().createLocation(locationSpec);
    }

    private LocationSpec<SshMachineLocation> buildLocationSpec(String applicationName, Object callerContext) {
        ApplicationDetail applicationDetail = getApplicationDetail(applicationName);
        String address = Iterables.getOnlyElement(applicationDetail.getUrls());
        Integer port = getSshPort();
        String sshCode = getCloudFoundryOperations().advanced().sshCode().block();

        return LocationSpec.create(SshMachineLocation.class)
                .configure("address", address)
                .configure(CloudFoundryLocationConfig.APPLICATION_NAME, applicationName)
                .configure(SshMachineLocation.PRIVATE_ADDRESSES, ImmutableList.of(address))
                .configure(CloudLocationConfig.USER, String.format("cf:%s/0", applicationDetail.getId()))
                .configure(SshMachineLocation.PASSWORD, sshCode)
                .configure(SshMachineLocation.SSH_PORT, port)
                .configure(BrooklynConfigKeys.SKIP_ON_BOX_BASE_DIR_RESOLUTION, true)
                .configure(BrooklynConfigKeys.ONBOX_BASE_DIR, "/tmp")
                .configure(CALLER_CONTEXT, callerContext);
    }

    private void pushApplication(PushApplicationRequest pushApplicationRequest) {
        getCloudFoundryOperations().applications().push(pushApplicationRequest).block();
    }

    private Integer getSshPort() {
        // see https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html#other-ssh-access
        GetInfoResponse info = getCloudFoundryClient().info().get(GetInfoRequest.builder().build()).block();
        String sshEndpoint = info.getApplicationSshEndpoint();
        return Integer.parseInt(Iterables.get(Splitter.on(":").split(sshEndpoint), 1));
    }

    private List getServiceInstancesFromManifest(Map<?, ?> manifestAsMap) {
        return (List) manifestAsMap.get("services");
    }

    private Entity lookUpEntityFromCallerContext(Object callerContext) {
        if (callerContext == null || !(callerContext instanceof Entity)) {
            throw new IllegalStateException("Invalid caller context: " + callerContext);
        }
        return  (Entity) callerContext;
    }


    private PushApplicationRequest createPushApplicationRequestFromVanillaCloudFoundryApplication(Entity entity) {
        String applicationName = entity.config().get(VanillaCloudFoundryApplication.APPLICATION_NAME);
        String domainName = entity.config().get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        int memory = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_MEMORY);
        int disk = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_DISK);
        int instances = entity.config().get(VanillaCloudFoundryApplication.REQUIRED_INSTANCES);
        String artifact = entity.config().get(VanillaCloudFoundryApplication.ARTIFACT_PATH);
        Path artifactLocalPath = getArtifactLocalPath(artifact);
        String buildpack = entity.config().get(VanillaCloudFoundryApplication.BUILDPACK);
        return createPushApplicationRequest(applicationName, memory, disk, artifactLocalPath, buildpack, domainName, instances);
    }

    private PushApplicationRequest createPushApplicationRequestFromManifest(Map<?, ?> manifestAsMap) {
        String applicationName = (String) manifestAsMap.get("name");
        String buildpack = (String) manifestAsMap.get("buildpack");
        Integer memory = MoreObjects.firstNonNull((Integer) manifestAsMap.get("memory"), 256);
        Integer disk = MoreObjects.firstNonNull((Integer) manifestAsMap.get("disk"), 512);
        String path = (String) manifestAsMap.get("path");
        String domain = (String) manifestAsMap.get("domain");
        Integer instances = MoreObjects.firstNonNull((Integer) manifestAsMap.get("instances"), 1);
        Path artifactLocalPath = getArtifactLocalPath(path);
        return createPushApplicationRequest(applicationName, memory, disk, artifactLocalPath, buildpack, domain, instances);
    }

    private String getManifestYamlFromEntity(Entity entity) {
        String configurationUrl = entity.getConfig(CONFIGURATION_URL);
        String configurationContents = entity.config().get(CONFIGURATION_CONTENTS);

        // Exactly one of the two must have a value
        if (isBlank(configurationUrl) == isBlank(configurationContents))
            throw new IllegalArgumentException("Exactly one of the two must have a value: '"
                    + CONFIGURATION_URL.getName() + "' or '" + CONFIGURATION_CONTENTS.getName() + "'.");

        if (!isBlank(configurationUrl)) {
            InputStream inputStream = new ResourceUtils(entity).getResourceFromUrl(configurationUrl);
            return getStringFromInputStream(inputStream);
        } else if (!isBlank(configurationContents)) {
                return configurationContents;
        } else {
            throw new IllegalStateException("Cannot find configurationUrl nor configurationContents in the entity");
        }
    }

    private Map<?, ?> getMapFromManifest(String yaml) {
        return Yamls.getAs(Yamls.parseAll(yaml), Map.class);
    }

    private static String getStringFromInputStream(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        String result;
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            result = byteArrayOutputStream.toString(Charsets.UTF_8.name());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return result;
    }

    private Path getArtifactLocalPath(String artifact) {
        if (artifact == null) return null;
        try {
            return new UrlResource(artifact).getFile().toPath();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void release(MachineLocation machine) {
        String applicationName = machine.config().get(CloudFoundryLocationConfig.APPLICATION_NAME);

        List<ServiceInstanceSummary> serviceInstanceSummaries = getCloudFoundryOperations().services().listInstances().collectList().block();
        List<String> instancesToBeDeleted = Lists.newArrayList();

        for (ServiceInstanceSummary serviceInstanceSummary : serviceInstanceSummaries) {
            for (String appName : serviceInstanceSummary.getApplications()) {
                if (applicationName.equalsIgnoreCase(appName)) {
                    instancesToBeDeleted.add(serviceInstanceSummary.getName());
                }
            }
        }

        getCloudFoundryOperations().applications().delete(DeleteApplicationRequest.builder()
                .name(applicationName)
                .deleteRoutes(true)
                .build()
        ).block();
        // delete service instances bound to the application
        for (String name : instancesToBeDeleted) {
            getCloudFoundryOperations().services().deleteInstance(
                    DeleteServiceInstanceRequest.builder()
                            .name(name).build())
                    .block();
        }
    }

    protected boolean isVanillaCloudFoundryApplication(Entity entity) {
        return entity.getEntityType().getName().equalsIgnoreCase(VanillaCloudFoundryApplication.class.getName());
    }

    protected boolean isCloudFoundryAppFromManifet(Entity entity) {
        return entity.getEntityType().getName().equalsIgnoreCase(CloudFoundryAppFromManifest.class.getName());
    }

    private List<String> createInstanceServices(List<Map<String, Object>> services) {
        List<String> serviceInstanceNames = Lists.newArrayList();
        for (Map<String, Object> service : services) {
            for (Map.Entry<String, Object> stringObjectEntry : service.entrySet()) {
                String serviceInstanceName = ((Map<String, String>)stringObjectEntry.getValue()).get("instanceName");
                serviceInstanceNames.add(serviceInstanceName);
                String planName = ((Map<String, String>)stringObjectEntry.getValue()).get("plan");
                Map<String, ?> parameters = (Map<String, ?>) ((Map<String, Object>)stringObjectEntry.getValue()).get("parameters");
                try {
                    getCloudFoundryOperations().services()
                            .createInstance(CreateServiceInstanceRequest.builder()
                                    .serviceName(stringObjectEntry.getKey())
                                    .serviceInstanceName(serviceInstanceName)
                                    .planName(planName)
                                    .parameters(parameters)
                                    .build())
                            .block();

                } catch (Exception e) {
                    LOG.error("Error creating the service {}, the error was {}", serviceInstanceName, e);
                    throw new PropagatedRuntimeException(e);
                }
            }
        }
        return serviceInstanceNames;
    }

    private PushApplicationRequest createPushApplicationRequest(String applicationName, int memory, int diskQuota, Path application, String buildpack, String domain, int instances) {
        return PushApplicationRequest.builder()
                .name(applicationName)
                .healthCheckType(ApplicationHealthCheck.NONE) // TODO is it needed?
                .randomRoute(true)
                .buildpack(buildpack)
                .application(application)
                .instances(instances)
                .domain(domain)
                .diskQuota(diskQuota)
                .memory(memory)
                .build();
    }

    private ApplicationDetail getApplicationDetail(String applicationName) {
        return getCloudFoundryOperations()
                .applications().get(
                        GetApplicationRequest.builder().name(applicationName).build())
                .block();
    }

    private void bindServices(String applicationName, List<String> serviceInstanceNames) {
        for (String serviceInstanceName : serviceInstanceNames) {
            try {
                getCloudFoundryOperations().services()
                        .bind(
                                BindServiceInstanceRequest.builder()
                                        .applicationName(applicationName)
                                        .serviceInstanceName(serviceInstanceName)
                                        .build()
                        ).block();
            } catch (Exception e) {
                LOG.error("Error getting environment for application {} the error was ", applicationName, e);
                throw new PropagatedRuntimeException(e);
            }
        }
    }

    private void restartApplication(String applicationName) {
        getCloudFoundryOperations().applications()
                .restart(
                        RestartApplicationRequest.builder()
                                .name(applicationName)
                                .build()
                ).block();
    }


    @Override
    public MachineProvisioningLocation<MachineLocation> newSubLocation(Map<?, ?> map) {
        return null;
    }

    @Override
    public Map<String, Object> getProvisioningFlags(Collection<String> collection) {
        return null;
    }

}
