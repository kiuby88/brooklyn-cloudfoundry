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
package org.apache.brooklyn.cloudfoundry.entity;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import org.apache.brooklyn.api.entity.drivers.downloads.DownloadResolver;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.cloudfoundry.utils.FileNameResolver;
import org.apache.brooklyn.cloudfoundry.utils.LocalResourcesDownloader;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.drivers.downloads.BasicDownloadResolver;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class VanillaPaasApplicationCloudFoundryDriver implements VanillaPaasApplicationDriver {

    public static final Logger log = LoggerFactory
            .getLogger(VanillaPaasApplicationCloudFoundryDriver.class);

    private final CloudFoundryPaasLocation location;
    private VanillaCloudFoundryApplicationImpl entity;
    private String applicationName;
    private String applicationUrl;
    protected String localArtifactPath;

    public VanillaPaasApplicationCloudFoundryDriver(VanillaCloudFoundryApplicationImpl entity,
                                                    CloudFoundryPaasLocation location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
        applicationName = entity.getApplicationName();
    }

    @Override
    public VanillaCloudFoundryApplicationImpl getEntity() {
        return entity;
    }

    @Override
    public CloudFoundryPaasLocation getLocation() {
        return location;
    }

    @Override
    public void start() {
        deploy();
        preLaunch();
        launch();
        postLaunch();
    }

    private String deploy() {
        Map<String, Object> params = MutableMap.copyOf(entity.config().getBag().getAllConfig());
        params.put(VanillaCloudFoundryApplication.APPLICATION_NAME.getName(), applicationName);
        String artifactPath = entity.getConfig(VanillaCloudFoundryApplication.ARTIFACT_PATH);
        localArtifactPath = getLocalPath(artifactPath);
        params.put(VanillaCloudFoundryApplication.ARTIFACT_PATH.getName(), localArtifactPath);

        applicationUrl = location.deploy(params);
        return applicationUrl;
    }

    private String getLocalPath(String artifactUrl) {
        DownloadResolver downloadResolver = new BasicDownloadResolver(ImmutableList.of(artifactUrl),
                FileNameResolver.findArchiveNameFromUrl(artifactUrl));
        try {
            File war;
            war = LocalResourcesDownloader
                    .downloadResourceInLocalDir(downloadResolver.getFilename(),
                            downloadResolver.getTargets());
            return war.getCanonicalPath();
        } catch (IOException e) {
            log.error("Error obtaining local path in {} for artifact {}",
                    this, downloadResolver.getTargets());
            throw Exceptions.propagate(e);
        }
    }

    protected void preLaunch() {
        configureEnv();
    }

    protected void configureEnv() {
        setEnv(entity.getConfig(VanillaCloudFoundryApplication.ENV));
    }

    @Override
    public void setEnv(Map<String, String> env) {
        if ((env != null) && (!env.isEmpty())) {
            location.setEnv(applicationName, env);
        }
        entity.sensors().set(VanillaCloudFoundryApplication.ENV,
                location.getEnv(applicationName));
    }

    private void launch() {
        location.startApplication(applicationName);
    }

    private void postLaunch() {
        entity.sensors().set(Attributes.MAIN_URI, URI.create(applicationUrl));
        entity.sensors().set(VanillaCloudFoundryApplication.ROOT_URL, applicationUrl);
        updateMemorySensor(location.getMemory(applicationName));
        updateDiskSensor(location.getDiskQuota(applicationName));
        updateInstancesSensor(location.getInstancesNumber(applicationName));
    }

    private void updateMemorySensor(int memory) {
        entity.sensors().set(VanillaCloudFoundryApplication.ALLOCATED_MEMORY, memory);
    }

    private void updateDiskSensor(int disk) {
        entity.sensors().set(VanillaCloudFoundryApplication.ALLOCATED_DISK, disk);
    }

    private void updateInstancesSensor(int instances) {
        entity.sensors().set(VanillaCloudFoundryApplication.INSTANCES, instances);
    }

    @Override
    public void restart() {
        location.restartApplication(applicationName);
    }

    @Override
    public void stop() {
        location.stopApplication(applicationName);
    }

    @Override
    public void delete() {
        location.deleteApplication(applicationName);
    }

    @Override
    public void rebind() {
        //TODO
    }

    @Override
    public void setMemory(int memory) {
        location.setMemory(applicationName, memory, localArtifactPath);
        updateMemorySensor(location.getMemory(applicationName));
    }

    @Override
    public void setDiskQuota(int diskQuota) {
        location.setDiskQuota(applicationName, diskQuota, localArtifactPath);
        updateDiskSensor(location.getDiskQuota(applicationName));
    }

    @Override
    public void setInstancesNumber(int instances) {
        location.setInstancesNumber(applicationName, instances, localArtifactPath);
        updateInstancesSensor(location.getInstancesNumber(applicationName));
    }

    public boolean isRunning() {
        return isApplicationDomainAvailable();
    }

    private boolean isApplicationDomainAvailable() {
        boolean result = false;
        try {
            result = HttpTool.getHttpStatusCode(applicationUrl) == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            log.warn("Application " + applicationName + "is not available yet for entity " + this);
        }
        return result;
    }

}
