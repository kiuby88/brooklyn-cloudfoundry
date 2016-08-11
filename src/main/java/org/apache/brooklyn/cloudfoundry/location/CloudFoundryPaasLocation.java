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

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.entity.services.VanillaCloudFoundryService;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.core.config.ResolvingConfigBag;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    private CloudFoundryOperations client;


    public enum AppState {

        UPDATING("UPDATING"),
        STARTED("STARTED"),
        STOPPED("STOPPED");

        private final String state;

        private AppState(final String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }
    }

    public CloudFoundryPaasLocation() {
        super();
    }

    public CloudFoundryPaasLocation(Map<?, ?> properties) {
        super(properties);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }

    protected CloudFoundryOperations getClient() {
        return getClient(MutableMap.of());
    }

    protected CloudFoundryOperations getClient(Map<?, ?> flags) {
        ConfigBag conf = (flags == null || flags.isEmpty())
                ? config().getBag()
                : ConfigBag.newInstanceExtending(config().getBag(), flags);
        return getClient(conf);
    }

    protected CloudFoundryOperations getClient(ConfigBag config) {
        if (client == null) {
            CloudFoundryClientRegistry registry = getConfig(CF_CLIENT_REGISTRY);
            client = registry.getCloudFoundryClient(
                    ResolvingConfigBag.newInstanceExtending(getManagementContext(), config), true);
        }
        return client;
    }

    public String deploy(Map<?, ?> params) {
        ConfigBag appSetUp = ConfigBag.newInstance(params);
        String artifact = checkNotNull(appSetUp.get(VanillaCloudFoundryApplication.ARTIFACT_PATH),
                VanillaCloudFoundryApplication.ARTIFACT_PATH.getName() + " can not be null");
        String name = appSetUp
                .get(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey());
        String buildpack = appSetUp.get(VanillaCloudFoundryApplication.BUILDPACK);
        Path artifactLocalPath =
                Paths.get(artifact);
        String host = appSetUp.get(VanillaCloudFoundryApplication.APPLICATION_HOST);

        String domain = appSetUp.get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        int memory = appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_MEMORY);
        int disk = appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_DISK);
        int instances = appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_INSTANCES);

        try {
            getClient().applications()
                    .push(PushApplicationRequest.builder()
                            .name(name)
                            .buildpack(buildpack)
                            .application(artifactLocalPath)
                            .host(host)
                            .noHostname(false)
                            .domain(domain)
                            .memory(memory)
                            .diskQuota(disk)
                            .instances(instances)
                            .healthCheckType(ApplicationHealthCheck.PORT)
                            .noStart(true)
                            .noRoute(false)
                            .build())
                    .doOnSuccess(v -> log.info("Done uploading for {} in {}", name, this))
                    .block(getConfig(OPERATIONS_TIMEOUT));
            return getApplicationUrl(name);
        } catch (Exception e) {
            log.error("Error creating application {}, error was {}", name, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    protected String getApplicationUrl(String applicationName) {
        return completeUrlProtocol(getApplicationUri(applicationName));
    }

    protected String getApplicationUri(String applicationName) {
        ApplicationDetail application = getApplication(applicationName);
        return Iterables.getOnlyElement(application.getUrls());
    }

    private String completeUrlProtocol(String baseUrl) {
        if ((!baseUrl.startsWith("https://"))
                && (!baseUrl.startsWith("http://"))) {
            baseUrl = "https://" + baseUrl;
        }
        return baseUrl;
    }

    private ApplicationDetail getApplication(final String applicationName) {
        try {
            return getClient().applications()
                    .get(GetApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error getting application {}, error was {}", applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void pushArtifact(String applicationName, String artifact) {
        try {
            getClient().applications()
                    .push(PushApplicationRequest.builder()
                            .name(applicationName)
                            .application(Paths.get(artifact))
                            .build())
                    .doOnSuccess(v -> log.info("Pushed artifact {}, for application " +
                            "{} in {}", new Object[]{artifact, applicationName, this}))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error pushing articat {} for application {}, error was {}",
                    new Object[]{artifact, applicationName, e});
            throw new PropagatedRuntimeException(e);
        }
    }

    public void startApplication(String applicationName) {
        try {
            getClient().applications()
                    .start(StartApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .doOnSuccess(v ->
                            log.info("Application {} was started correctly", applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error starting application {}, error was {}", applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void stopApplication(String applicationName) {
        try {
            getClient().applications()
                    .stop(StopApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .doOnSuccess(v ->
                            log.info("Application {} was stopped correctly", applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.info("Error stopping application {}, error was {}",
                    applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void restartApplication(String applicationName) {
        try {
            getClient().applications()
                    .restart(RestartApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .doOnSuccess(v ->
                            log.info("Application {} was restarted correctly", applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.info("Error restarting application {}, error was {}", applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void deleteApplication(String applicationName) {
        try {
            getClient().applications()
                    .delete(DeleteApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .doOnSuccess(v ->
                            log.info("Application {} was deleted correctly", applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.info("Error deleting application {}, error was {}", applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void setEnv(String applicationName, Map<String, String> env) {
        if (env != null) {
            for (Map.Entry<String, String> envEntry : env.entrySet()) {
                setEnv(applicationName, envEntry.getKey(), String.valueOf(envEntry.getValue()));
            }
        }
    }

    public void setEnv(String applicationName, String variableName, String variableValue) {
        try {
            getClient().applications()
                    .setEnvironmentVariable(SetEnvironmentVariableApplicationRequest.builder()
                            .name(applicationName)
                            .variableName(variableName)
                            .variableValue(variableValue)
                            .build())
                    .doOnSuccess(v -> log.info("Setting env {} with value {} for application {}",
                            new Object[]{variableName, variableValue, applicationName}))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error setting env {} with value {} for  application" +
                    " {} the error was {}", new Object[]{variableName, variableValue,
                    applicationName, e});
            throw new PropagatedRuntimeException(e);
        }
    }

    public Map<String, String> getEnv(String applicationName) {
        try {
            Map<String, Object>
                    userProvided = getClient().applications()
                    .getEnvironments(GetApplicationEnvironmentsRequest.builder()
                            .name(applicationName)
                            .build())
                    .doOnSuccess(v -> log.info("Getting env for application {}", applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT))
                    .getUserProvided();
            return mapOfStrings(userProvided);
        } catch (Exception e) {
            log.error("Error getting env for application {} the error was ", applicationName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    private Map<String, String> mapOfStrings(Map<String, Object> map) {
        Map<String, String> result = MutableMap.of();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    public AppState getApplicationStatus(String applicationName) {
        return AppState.valueOf(getApplication(applicationName).getRequestedState());
    }

    public boolean isDeployed(String applicationName) {
        boolean result;
        try {
            result = getApplication(applicationName) != null;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public void setMemory(String applicationName, int memory) {
        try {
            getClient().applications()
                    .scale(ScaleApplicationRequest.builder()
                            .name(applicationName)
                            .memoryLimit(memory)
                            .build())
                    .doOnSuccess(v -> log.info("Setting memory {} for application {}",
                            memory, applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error setting memory {} for application {} the error was {}",
                    new Object[]{memory, applicationName, e});
            throw new PropagatedRuntimeException(e);
        }
    }

    public void setDiskQuota(String applicationName, final int diskQuota) {
        try {
            getClient().applications()
                    .scale(ScaleApplicationRequest.builder()
                            .name(applicationName)
                            .diskLimit(diskQuota)
                            .build())
                    .doOnSuccess(v -> log.info("Setting diskQouta {} for application {}",
                            diskQuota, applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error setting diskQuota {} for application {} the error was {}",
                    new Object[]{diskQuota, applicationName, e});
            throw new PropagatedRuntimeException(e);
        }
    }

    public void setInstancesNumber(String applicationName, final int instances) {
        try {
            getClient().applications()
                    .scale(ScaleApplicationRequest.builder()
                            .name(applicationName)
                            .instances(instances)
                            .build())
                    .doOnSuccess(v -> log.info("Setting instances {} for application {}",
                            instances, applicationName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error setting instances {} for application {} the error was {}",
                    new Object[]{instances, applicationName, e});
            throw new PropagatedRuntimeException(e);
        }
    }

    public int getInstancesNumber(String applicationName) {
        return getApplication(applicationName).getInstances();
    }

    public int getDiskQuota(String applicationName) {
        return getApplication(applicationName).getDiskQuota();
    }

    public int getMemory(String applicationName) {
        return getApplication(applicationName).getMemoryLimit();
    }

    public void createServiceInstance(Map<?, ?> params) {
        ConfigBag serviceSetUp = ConfigBag.newInstance(params);
        String serviceName = checkNotNull(serviceSetUp.get(VanillaCloudFoundryService.SERVICE_NAME),
                "Service Name can not be null");
        String serviceInstanceName = checkNotNull(
                serviceSetUp.get(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME),
                "Service Instance Name can not be null");
        String plan = checkNotNull(
                serviceSetUp.get(VanillaCloudFoundryService.PLAN),
                "Plan can not be null");
        try {
            getClient().services()
                    .createInstance(CreateServiceInstanceRequest.builder()
                            .serviceName(serviceName)
                            .serviceInstanceName(serviceInstanceName)
                            .planName(plan)
                            .build())
                    .doOnSuccess(v ->
                            log.info("Service {} was created correctly", serviceInstanceName))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error creating the service {}, the error was {}", serviceInstanceName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public boolean serviceInstanceExist(String serviceInstanceName) {
        boolean result;
        try {
            result = getServiceInstance(serviceInstanceName) != null;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public ServiceInstance getServiceInstance(String serviceInstanceName) {
        try {
            return getClient().services()
                    .getInstance(GetServiceInstanceRequest.builder()
                            .name(serviceInstanceName)
                            .build())
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error gettin the service {} the error was {}", serviceInstanceName, e);
            throw new PropagatedRuntimeException(e);
        }
    }

    public void deleteServiceInstance(String serviceInstanceId) {
        try {
            getClient().services()
                    .deleteInstance(DeleteServiceInstanceRequest.builder()
                            .name(serviceInstanceId)
                            .build())
                    .doOnSuccess(v -> log.info("Deleted service instance {}", serviceInstanceId))
                    .block(getConfig(OPERATIONS_TIMEOUT));
        } catch (Exception e) {
            log.error("Error deleting service {}, the error was {}", serviceInstanceId, e);
        }
    }

}
