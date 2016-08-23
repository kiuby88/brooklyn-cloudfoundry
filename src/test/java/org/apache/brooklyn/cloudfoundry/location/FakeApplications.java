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

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationEvent;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSshEnabledRequest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.CopySourceApplicationRequest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.DisableApplicationSshRequest;
import org.cloudfoundry.operations.applications.EnableApplicationSshRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationEventsRequest;
import org.cloudfoundry.operations.applications.GetApplicationHealthCheckRequest;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.RenameApplicationRequest;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.applications.SetApplicationHealthCheckRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.applications.UnsetEnvironmentVariableApplicationRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FakeApplications implements Applications {

    private static final String DEFAULT_DOMAIN = AbstractCloudFoundryUnitTest.BROOKLYN_DOMAIN;
    private static final String DEFAULT_STACK = "cflinuxfs2";
    private static final String STARTED = "STARTED";
    private static final String STOPPED = "STOPPED";
    private static final int ID_SIZE = 36;

    Map<String, ApplicationDetail> applications;
    Map<String, Map<String, String>> applicationEnv;

    public FakeApplications() {
        applications = MutableMap.of();
        applicationEnv = MutableMap.of();
    }

    @Override
    public Mono<Void> push(PushApplicationRequest request) {
        checkApplicationPath(request);
        String name = request.getName();
        if (!applications.containsKey(name)) {
            manageNewReques(request);
        } else {
            updateApplication(request);
        }
        return Mono.empty();
    }

    boolean containsApplication(String applicationName) {
        return applications.containsKey(applicationName);
    }

    private void checkApplicationPath(PushApplicationRequest request) {
        if (request.getApplication() == null) {
            throw new IllegalStateException("Cannot build PushApplicationRequest, " +
                    "some of required attributes are not set [application]");
        }
        Path artifactPath = request.getApplication();
        if (!Files.exists(artifactPath) || artifactPath.toString().equals(Strings.EMPTY)) {
            throw new FileSystemNotFoundException("Not found: " + artifactPath.toString());
        }
    }

    private void manageNewReques(PushApplicationRequest request) {
        applications.put(request.getName(), ApplicationDetail.builder()
                .name(request.getName())
                .buildpack(request.getBuildpack())
                .stack(DEFAULT_STACK)
                .id(generateApplicationId())
                .memoryLimit(request.getMemory())
                .diskQuota(request.getDiskQuota())
                .instances(request.getInstances())
                .runningInstances(request.getInstances())
                .requestedState(STOPPED)
                .url(getUrl(request))
                .build());
        initEnv(request);
    }

    private String generateApplicationId() {
        return Strings.makeRandomId(ID_SIZE);
    }

    private String getUrl(PushApplicationRequest request) {
        return composeApplicationUrl(findHostName(request), getValidDomain(request));
    }

    private String findHostName(PushApplicationRequest request) {
        String host = request.getHost();
        if (Strings.isBlank(host)) {
            host = request.getName();
        }
        return host;
    }

    private String getValidDomain(PushApplicationRequest request) {
        String domain = request.getDomain();
        if (Strings.isBlank(domain) || domain.equals(DEFAULT_DOMAIN)) {
            return DEFAULT_DOMAIN;
        }
        throw new IllegalStateException("Domain " + domain + " not found");
    }

    private String composeApplicationUrl(String host, String domain) {
        return host + "." + domain;
    }

    private void initEnv(PushApplicationRequest request) {
        applicationEnv.put(request.getName(), MutableMap.<String, String>of());
    }

    private void updateApplication(PushApplicationRequest request) {
        updateMemory(request);
        updateDiskQuota(request);
        updateInstances(request);
    }

    private void updateMemory(PushApplicationRequest request) {
        if (request.getMemory() != null) {
            String applicationName = request.getName();
            ApplicationDetail baseApplicationDetail = applications.get(applicationName);
            applications.put(request.getName(), ApplicationDetail.builder()
                    .from(baseApplicationDetail)
                    .memoryLimit(request.getMemory())
                    .build());
        }
    }

    private void updateDiskQuota(PushApplicationRequest request) {
        if (request.getDiskQuota() != null) {
            String applicationName = request.getName();
            ApplicationDetail baseApplicationDetail = applications.get(applicationName);
            applications.put(request.getName(), ApplicationDetail.builder()
                    .from(baseApplicationDetail)
                    .diskQuota(request.getDiskQuota())
                    .build());
        }
    }

    private void updateInstances(PushApplicationRequest request) {
        if (request.getInstances() != null) {
            String applicationName = request.getName();
            ApplicationDetail baseApplicationDetail = applications.get(applicationName);
            applications.put(request.getName(), ApplicationDetail.builder()
                    .from(baseApplicationDetail)
                    .instances(request.getInstances())
                    .build());
        }
    }

    @Override
    public Mono<Void> copySource(CopySourceApplicationRequest request) {
        return null;
    }

    @Override
    public Mono<Void> delete(DeleteApplicationRequest request) {
        ApplicationDetail application = getApplication(request.getName());
        applications.remove(application.getName());
        applicationEnv.remove(application.getName());
        return Mono.empty();
    }

    @Override
    public Mono<Void> disableSsh(DisableApplicationSshRequest request) {
        return null;
    }

    @Override
    public Mono<Void> enableSsh(EnableApplicationSshRequest request) {
        return null;
    }

    @Override
    public Mono<ApplicationDetail> get(GetApplicationRequest request) {
        return Mono.just(getApplication(request.getName()));
    }

    @Override
    public Mono<ApplicationManifest> getApplicationManifest(GetApplicationManifestRequest request) {
        return null;
    }

    @Override
    public Mono<ApplicationEnvironments> getEnvironments(GetApplicationEnvironmentsRequest request) {
        ApplicationDetail application = getApplication(request.getName());
        Map<String, String> userEnv = applicationEnv.get(application.getName());
        return Mono.just(ApplicationEnvironments.builder()
                .userProvided(userEnv).build());
    }

    @Override
    public Flux<ApplicationEvent> getEvents(GetApplicationEventsRequest request) {
        return null;
    }

    @Override
    public Mono<ApplicationHealthCheck> getHealthCheck(GetApplicationHealthCheckRequest request) {
        return null;
    }

    @Override
    public Flux<ApplicationSummary> list() {
        return null;
    }

    @Override
    public Flux<LogMessage> logs(LogsRequest request) {
        return null;
    }

    @Override
    public Mono<Void> rename(RenameApplicationRequest request) {
        return null;
    }

    @Override
    public Mono<Void> restage(RestageApplicationRequest request) {
        return null;
    }

    @Override
    public Mono<Void> restart(RestartApplicationRequest request) {
        setStartedState(request.getName());
        return Mono.empty();
    }

    @Override
    public Mono<Void> restartInstance(RestartApplicationInstanceRequest request) {
        return null;
    }

    @Override
    public Mono<Void> scale(ScaleApplicationRequest request) {
        String name = request.getName();
        ApplicationDetail application = applications.get(name);
        ApplicationDetail.Builder builder = ApplicationDetail.builder().from(application);
        if (request.getMemoryLimit() != null) {
            builder.memoryLimit(request.getMemoryLimit());
        }
        if (request.getDiskLimit() != null) {
            builder.diskQuota(request.getDiskLimit());
        }
        if (request.getInstances() != null) {
            builder.instances(request.getInstances());
        }
        applications.put(name, builder.build());
        return Mono.empty();
    }

    @Override
    public Mono<Void> setEnvironmentVariable(SetEnvironmentVariableApplicationRequest request) {
        ApplicationDetail application = applications.get(request.getName());
        applicationEnv.get(application.getName())
                .put(request.getVariableName(), request.getVariableValue());
        return Mono.empty();
    }

    @Override
    public Mono<Void> setHealthCheck(SetApplicationHealthCheckRequest request) {
        return null;
    }

    @Override
    public Mono<Boolean> sshEnabled(ApplicationSshEnabledRequest request) {
        return null;
    }

    @Override
    public Mono<Void> start(StartApplicationRequest request) {
        setStartedState(request.getName());
        return Mono.empty();
    }

    private Void setStartedState(String name) {
        ApplicationDetail baseApplication = getApplication(name);
        applications.put(name, ApplicationDetail.builder()
                .from(baseApplication)
                .requestedState(STARTED).build());
        return null;
    }

    private Void setStoppedState(String name) {
        ApplicationDetail baseApplication = getApplication(name);
        applications.put(name, ApplicationDetail.builder()
                .from(baseApplication)
                .requestedState(STOPPED).build());
        return null;
    }

    private ApplicationDetail getApplication(String name) {
        if (!applications.containsKey(name)) {
            throw new IllegalStateException(" Application " + name + " does not exist");
        }
        return applications.get(name);
    }

    @Override
    public Mono<Void> stop(StopApplicationRequest request) {
        setStoppedState(request.getName());
        return Mono.empty();
    }

    @Override
    public Mono<Void> unsetEnvironmentVariable(UnsetEnvironmentVariableApplicationRequest request) {
        return null;
    }
}
