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
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryEntityImpl;
import org.apache.brooklyn.cloudfoundry.entity.utils.LocalResourcesDownloader;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryWebAppImpl extends CloudFoundryEntityImpl
        implements CloudFoundryWebApp {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryWebAppImpl.class);
    private static final String DOMAIN_KEYWORD = "-domain.";
    private String applicationName;
    private String artifactUrl;

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
        getCloudFoundryLocation();

        List<String> uris = MutableList.of();
        Staging staging;
        File war;
        try {
            staging = new Staging(null, getBuildpack());
            uris.add(inferApplicationDomainUri(getApplicationName()));

            war = LocalResourcesDownloader
                    .downloadResourceInLocalDir(getArtifactUrl());

            getClient().createApplication(getApplicationName(), staging,
                    getCloudFoundryLocation().getConfig(CloudFoundryPaasLocation.REQUIRED_MEMORY),
                    uris, null);
            getClient().uploadApplication(getApplicationName(), war.getCanonicalPath());
        } catch (IOException e) {
            log.error("Error deploying application {} ", this);
            throw Exceptions.propagate(e);
        }
    }

    private void preLaunch() {
        configureEnv();
    }

    private void launch() {
        getClient().startApplication(applicationName);
    }

    private void postLaunch() {
        waitForEntityStart();
        String domainUri = getDomainUri();
        sensors().set(Attributes.MAIN_URI, URI.create(domainUri));
        sensors().set(CloudFoundryWebApp.ROOT_URL, domainUri);
    }

    private void configureEnv() {
        //TODO a sensor with the custom-environment variables?
    }

    private String inferApplicationDomainUri(String name) {
        String defaultDomainName = getClient().getDefaultDomain().getName();
        return name + DOMAIN_KEYWORD + defaultDomainName;
    }

    private String getDomainUri() {
        String domainUri = null;
        CloudApplication application = getClient().getApplication(applicationName);
        if (application != null) {
            domainUri = "https://" + application.getUris().get(0);
        }
        return domainUri;
    }

    public boolean isRunning() {
        try {
            CloudApplication app = getClient().getApplication(applicationName);
            return (app != null)
                    && app.getState().equals(CloudApplication.AppState.STARTED)
                    && isApplicationDomainAvailable();
        } catch (Exception e) {
            return false;
        }
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

    private CloudFoundryClient getClient() {
        return getCloudFoundryLocation().getCloudFoundryClient();
    }

    private String getApplicationName() {
        return applicationName;
    }

    private String getArtifactUrl() {
        return artifactUrl;
    }

}
