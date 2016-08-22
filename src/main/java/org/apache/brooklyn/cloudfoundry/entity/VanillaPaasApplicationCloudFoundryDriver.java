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


import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.drivers.downloads.DownloadResolver;
import org.apache.brooklyn.cloudfoundry.entity.service.AfterBindingOperations;
import org.apache.brooklyn.cloudfoundry.entity.service.CloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.cloudfoundry.utils.FileNameResolver;
import org.apache.brooklyn.cloudfoundry.utils.LocalResourcesDownloader;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.drivers.downloads.BasicDownloadResolver;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.flags.TypeCoercions;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class VanillaPaasApplicationCloudFoundryDriver extends EntityPaasCloudFoundryDriver
        implements VanillaPaasApplicationDriver {

    public static final Logger log = LoggerFactory
            .getLogger(VanillaPaasApplicationCloudFoundryDriver.class);

    private String applicationName;
    private String applicationUrl;

    public VanillaPaasApplicationCloudFoundryDriver(VanillaCloudFoundryApplicationImpl entity,
                                                    CloudFoundryPaasLocation location) {
        super(entity, location);
        applicationName = entity.getApplicationName();
    }

    @Override
    public VanillaCloudFoundryApplicationImpl getEntity() {
        return (VanillaCloudFoundryApplicationImpl) super.getEntity();
    }

    @Override
    public void start() {
        deploy();
        preLaunch();
        launch();
        postLaunch();
    }

    private String deploy() {
        Map<String, Object> params =
                MutableMap.copyOf(getEntity().config().getBag().getAllConfig());
        params.put(VanillaCloudFoundryApplication.APPLICATION_NAME.getName(), applicationName);
        String artifactPropertyName = VanillaCloudFoundryApplication.ARTIFACT_PATH.getName();
        String artifactPath = (String) params.get(artifactPropertyName);
        if (!Strings.isBlank(artifactPath)) {
            params.put(artifactPropertyName, getLocalPath(artifactPath));
        }

        applicationUrl = getLocation().deploy(params);
        return applicationUrl;
    }

    private String getLocalPath(String artifactPath) {
        DownloadResolver downloadResolver = getDownloadResolver(artifactPath);
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

    private DownloadResolver getDownloadResolver(String artifactPath) {
        return new BasicDownloadResolver(ImmutableList.of(artifactPath),
                FileNameResolver.findArchiveNameFromUrl(artifactPath));
    }

    protected void preLaunch() {
        bindServices();
        configureEnv();
    }

    private void bindServices() {
        List<String> services = getEntity().getConfig(VanillaCloudFoundryApplication.SERVICES);
        for (String serviceInstanceId : services) {
            bindService(serviceInstanceId);
        }
    }

    private void bindService(String serviceInstanceId) {
        Optional<CloudFoundryService> optinalService =
                findServiceEntitiesByInstanceName(serviceInstanceId);
        if (optinalService.isPresent()) {
            bindServiceFromEntity(optinalService.get());
        } else {
            bindServiceFromId(serviceInstanceId);
        }
    }

    private void bindServiceFromEntity(CloudFoundryService serviceEntity) {
        Entities.waitForServiceUp(serviceEntity);
        bindServiceFromId(serviceEntity
                .getAttribute(CloudFoundryService.SERVICE_INSTANCE_NAME));
        if (serviceEntity instanceof AfterBindingOperations) {
            ((AfterBindingOperations) serviceEntity).operationAfterBindingTo(applicationName);
        }
    }

    private void bindServiceFromId(String serviceInstanceId) {
        getLocation().bindServiceToApplication(serviceInstanceId, applicationName);
    }

    private Optional<CloudFoundryService> findServiceEntitiesByInstanceName(String serviceInstanceName) {
        Application root = getEntity().getApplication().getApplication();
        Optional<Entity> optional = Iterables
                .tryFind(Entities.descendantsWithoutSelf(root), new Predicate<Entity>() {
                    @Override
                    public boolean apply(@Nullable Entity input) {
                        Maybe<CloudFoundryService> maybe =
                                TypeCoercions.tryCoerce(input, CloudFoundryService.class);
                        return maybe.isPresent()
                                && instanceName(maybe.get(), serviceInstanceName);
                    }
                });

        if (optional.isPresent()) {
            CloudFoundryService service =
                    TypeCoercions.coerce(optional.get(), CloudFoundryService.class);
            return Optional.of(service);
        }
        return Optional.absent();
    }

    private boolean instanceName(CloudFoundryService service, String instanceName) {
        return service.getAttribute(CloudFoundryService.SERVICE_INSTANCE_NAME)
                .equals(instanceName);
    }

    protected void configureEnv() {
        setEnv(getEntity().getConfig(VanillaCloudFoundryApplication.ENV));
    }

    @Override
    public void setEnv(Map<String, String> env) {
        if ((env != null) && (!env.isEmpty())) {
            getLocation().setEnv(applicationName, env);
        }
        getEntity().sensors().set(VanillaCloudFoundryApplication.ENV,
                getLocation().getEnv(applicationName));
    }

    private void launch() {
        getLocation().startApplication(applicationName);
    }

    private void postLaunch() {
        getEntity().sensors().set(Attributes.MAIN_URI, URI.create(applicationUrl));
        getEntity().sensors().set(VanillaCloudFoundryApplication.ROOT_URL, applicationUrl);
        updateMemorySensor(getLocation().getMemory(applicationName));
        updateDiskSensor(getLocation().getDiskQuota(applicationName));
        updateInstancesSensor(getLocation().getInstancesNumber(applicationName));
    }

    private void updateMemorySensor(int memory) {
        getEntity().sensors().set(VanillaCloudFoundryApplication.ALLOCATED_MEMORY, memory);
    }

    private void updateDiskSensor(int disk) {
        getEntity().sensors().set(VanillaCloudFoundryApplication.ALLOCATED_DISK, disk);
    }

    private void updateInstancesSensor(int instances) {
        getEntity().sensors().set(VanillaCloudFoundryApplication.INSTANCES, instances);
    }

    @Override
    public void restart() {
        getLocation().restartApplication(applicationName);
    }

    @Override
    public void stop() {
        getLocation().stopApplication(applicationName);
    }

    @Override
    public void delete() {
        getLocation().deleteApplication(applicationName);
    }

    @Override
    public void rebind() {
        //TODO
    }

    @Override
    public void setMemory(int memory) {
        getLocation().setMemory(applicationName, memory);
        updateMemorySensor(getLocation().getMemory(applicationName));
    }

    @Override
    public void setDiskQuota(int diskQuota) {
        getLocation().setDiskQuota(applicationName, diskQuota);
        updateDiskSensor(getLocation().getDiskQuota(applicationName));
    }

    @Override
    public void setInstancesNumber(int instances) {
        getLocation().setInstancesNumber(applicationName, instances);
        updateInstancesSensor(getLocation().getInstancesNumber(applicationName));
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
