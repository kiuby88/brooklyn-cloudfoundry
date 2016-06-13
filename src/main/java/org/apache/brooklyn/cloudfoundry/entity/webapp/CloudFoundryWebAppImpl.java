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
package org.apache.brooklyn.cloudfoundry.entity.webapp;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryEntityImpl;
import org.apache.brooklyn.cloudfoundry.entity.utils.LocalResourcesDownloader;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryWebAppImpl extends CloudFoundryEntityImpl
        implements CloudFoundryWebApp {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryWebAppImpl.class);
    private String applicationName;
    private String artifactUrl;
    private String applicationDomanin;

    public CloudFoundryWebAppImpl() {
        super(MutableMap.of(), null);
    }

    public CloudFoundryWebAppImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public CloudFoundryWebAppImpl(Map properties) {
        this(properties, null);
    }

    public CloudFoundryWebAppImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public void init() {
        super.init();
        initApplicationParameters();
    }

    private void initApplicationParameters() {
        if (!Strings.isBlank(getConfig(CloudFoundryWebApp.APPLICATION_NAME))) {
            applicationName = getConfig(CloudFoundryWebApp.APPLICATION_NAME);
        } else {
            applicationName = "cf-app-" + Identifiers.makeRandomId(8);
        }
        artifactUrl = getConfig(CloudFoundryWebApp.ARTIFACT_URL);
        applicationDomanin = Strings.EMPTY;
    }

    @Override
    public String getBuildpack() {
        return "https://github.com/cloudfoundry/java-buildpack.git";
    }

    @Override
    protected void customStart() {
        deploy();
        preLaunch();
        launch();
        postLaunch();
    }

    private void deploy() {
        try {
            File war;
            war = LocalResourcesDownloader
                    .downloadResourceInLocalDir(getArtifactUrl());
            String path = war.getCanonicalPath();
            applicationDomanin =
                    getCloudFoundryLocation().deploy(applicationName, getBuildpack(), path);
        } catch (IOException e) {
            log.error("Error deploying application {} ", this);
            throw Exceptions.propagate(e);
        }
    }

    private void preLaunch() {
        configureEnv();
    }

    private void configureEnv() {
        //TODO
    }

    private void launch() {
        getCloudFoundryLocation().startApplication(applicationName);
    }

    private void postLaunch() {
        waitForEntityStart();
        String domainUri = getDomainUri();
        sensors().set(Attributes.MAIN_URI, URI.create(domainUri));
        sensors().set(CloudFoundryWebApp.ROOT_URL, domainUri);
    }

    @Override
    protected void customStop() {
        getCloudFoundryLocation().stopApplication(applicationName);
        deleteApplication();
    }

    private void deleteApplication() {
        getCloudFoundryLocation().deleteApplication(applicationName);
    }

    public boolean isRunning() {
        return isApplicationDomainAvailable();
    }

    private boolean isApplicationDomainAvailable() {
        boolean result;
        try {
            result = HttpTool.getHttpStatusCode(getDomainUri()) == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private String getArtifactUrl() {
        return artifactUrl;
    }

    private String getDomainUri() {
        return applicationDomanin;
    }

}
