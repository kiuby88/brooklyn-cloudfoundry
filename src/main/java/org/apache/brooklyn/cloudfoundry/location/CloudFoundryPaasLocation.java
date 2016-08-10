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
import java.util.concurrent.ExecutionException;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
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
        String applicationName = appSetUp
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
                            .name(applicationName)
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
                    .toFuture()
                    .get();
            return getApplicationUrl(applicationName);
        } catch (Exception e) {
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
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
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
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void startApplication(String applicationName) {
        try {
            getClient().applications()
                    .start(StartApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void stopApplication(String applicationName) {
        try {
            getClient().applications()
                    .stop(StopApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void restartApplication(String applicationName) {
        try {
            getClient().applications()
                    .restart(RestartApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void deleteApplication(String applicationName) {
        try {
            getClient().applications()
                    .delete(DeleteApplicationRequest.builder()
                            .name(applicationName)
                            .build())
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
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
                    .toFuture()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
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
                    .toFuture()
                    .get()
                    .getUserProvided();
            return mapOfStrings(userProvided);
        } catch (ExecutionException | InterruptedException e) {
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
                    .toFuture()
                    .get();
        } catch (Exception e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void setDiskQuota(String applicationName, int diskQuota) {
        try {
            getClient().applications()
                    .scale(ScaleApplicationRequest.builder()
                            .name(applicationName)
                            .diskLimit(diskQuota)
                            .build())
                    .toFuture()
                    .get();
        } catch (Exception e) {
            throw new PropagatedRuntimeException(e);
        }
    }

    public void setInstancesNumber(String applicationName, int instances) {
        try {
            getClient().applications()
                    .scale(ScaleApplicationRequest.builder()
                            .name(applicationName)
                            .instances(instances)
                            .build())
                    .toFuture()
                    .get();
        } catch (Exception e) {
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

}
