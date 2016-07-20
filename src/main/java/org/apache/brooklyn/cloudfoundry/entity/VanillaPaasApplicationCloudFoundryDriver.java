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

import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.cloudfoundry.utils.LocalResourcesDownloader;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaPaasApplicationCloudFoundryDriver implements VanillaPaasApplicationDriver {

    public static final Logger log = LoggerFactory
            .getLogger(VanillaPaasApplicationCloudFoundryDriver.class);

    private final CloudFoundryPaasLocation location;
    private VanillaCloudfoundryApplicationImpl entity;
    private String applicationName;
    private String applicationUrl;

    public VanillaPaasApplicationCloudFoundryDriver(VanillaCloudfoundryApplicationImpl entity,
                                                    CloudFoundryPaasLocation location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
        applicationName = entity.getApplicationName();
    }

    @Override
    public VanillaCloudfoundryApplicationImpl getEntity() {
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
        params.put(VanillaCloudfoundryApplication.APPLICATION_NAME.getName(), applicationName);
        if (params.containsKey(VanillaCloudfoundryApplication.ARTIFACT_PATH.getName())) {
            String path = (String) params
                    .get(VanillaCloudfoundryApplication.ARTIFACT_PATH.getName());
            params.put(VanillaCloudfoundryApplication.ARTIFACT_PATH.getName(), getLocalPath(path));
        }
        applicationUrl = location.deploy(params);
        return applicationUrl;
    }

    private String getLocalPath(String uri) {
        try {
            File war;
            war = LocalResourcesDownloader
                    .downloadResourceInLocalDir(uri);
            return war.getCanonicalPath();
        } catch (IOException e) {
            log.error("Error obtaining local path in {} for artifact {}", this, uri);
            throw Exceptions.propagate(e);
        }
    }

    protected void preLaunch() {
        configureEnv();
    }

    protected void configureEnv() {
        setEnv(entity.getConfig(VanillaCloudfoundryApplication.ENV));
    }

    @Override
    public void setEnv(Map<String, String> env) {
        if ((env != null) && (!env.isEmpty())) {
            location.setEnv(applicationName, env);
        }
        entity.sensors().set(VanillaCloudfoundryApplication.APPLICATION_ENV,
                location.getEnv(applicationName));
    }

    private void launch() {
        location.startApplication(applicationName);
    }

    private void postLaunch() {
        entity.sensors().set(Attributes.MAIN_URI, URI.create(applicationUrl));
        entity.sensors().set(VanillaCloudfoundryApplication.ROOT_URL, applicationUrl);
        updateMemorySensor(entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_MEMORY));
        updateDiskSensor(entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_DISK));
        updateInstancesSensor(entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_INSTANCES));
    }

    private void updateMemorySensor(int memory) {
        entity.sensors().set(VanillaCloudfoundryApplication.ALLOCATED_MEMORY, memory);
    }

    private void updateDiskSensor(int disk) {
        entity.sensors().set(VanillaCloudfoundryApplication.ALLOCATED_DISK, disk);
    }

    private void updateInstancesSensor(int instances) {
        entity.sensors().set(VanillaCloudfoundryApplication.INSTANCES, instances);
    }

    @Override
    public void restart() {
        //TODO
    }

    @Override
    public void stop() {
        location.stop(applicationName);
    }

    @Override
    public void delete() {
        location.delete(applicationName);
    }

    @Override
    public void rebind() {
        //TODO
    }

    @Override
    public void setMemory(int memory) {
        location.setMemory(applicationName, memory);
        updateMemorySensor(location.getMemory(applicationName));
    }

    @Override
    public void setDiskQuota(int diskQuota) {
        location.setDiskQuota(applicationName, diskQuota);
        updateDiskSensor(location.getDiskQuota(applicationName));
    }

    @Override
    public void setInstancesNumber(int instancesNumber) {
        location.setInstancesNumber(applicationName, instancesNumber);
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
