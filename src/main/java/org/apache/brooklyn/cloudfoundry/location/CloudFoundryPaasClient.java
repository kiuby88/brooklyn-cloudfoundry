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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudfoundryApplication;
import org.apache.brooklyn.cloudfoundry.utils.LocalResourcesDownloader;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class CloudFoundryPaasClient {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasClient.class);


    private final CloudFoundryPaasLocation location;
    private CloudFoundryClient client;

    public CloudFoundryPaasClient(CloudFoundryPaasLocation location) {
        this.location = location;
    }

    protected CloudFoundryClient getClient() {
        if (client == null) {
            CloudCredentials credentials =
                    new CloudCredentials(
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_IDENTITY),
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_CREDENTIAL));
            client = new CloudFoundryClient(credentials,
                    getTargetURL(location.getConfig(CloudFoundryPaasLocationConfig.CLOUD_ENDPOINT)),
                    location.getConfig(CloudFoundryPaasLocationConfig.CF_ORG),
                    location.getConfig(CloudFoundryPaasLocationConfig.CF_SPACE), true);

            client.login();
        }
        return client;
    }

    public String deploy(Map<?, ?> params) {
        ConfigBag appSetUp = ConfigBag.newInstance(params);
        String artifactLocalPath =
                getLocalPath(appSetUp.get(VanillaCloudfoundryApplication.ARTIFACT_PATH));
        String applicationName = appSetUp.get(VanillaCloudfoundryApplication.APPLICATION_NAME);

        getClient().createApplication(applicationName, getStaging(appSetUp),
                appSetUp.get(VanillaCloudfoundryApplication.REQUIRED_DISK),
                appSetUp.get(VanillaCloudfoundryApplication.REQUIRED_MEMORY),
                getUris(appSetUp), null);

        getClient().updateApplicationInstances(applicationName,
                appSetUp.get(VanillaCloudfoundryApplication.REQUIRED_INSTANCES));

        pushArtifact(applicationName, artifactLocalPath);
        return getDomainUri(applicationName);
    }

    protected Staging getStaging(ConfigBag config) {
        String buildpack = config.get(VanillaCloudfoundryApplication.BUILDPACK);
        return new Staging(null, buildpack);
    }

    protected List<String> getUris(ConfigBag config) {
        return MutableList.of(inferApplicationRouteUri(config));
    }

    private String inferApplicationRouteUri(ConfigBag config) {
        String domain = config.get(VanillaCloudfoundryApplication.APPLICATION_DOMAIN);
        if (Strings.isBlank(domain)) {
            domain = getClient().getDefaultDomain().getName();
        }
        if(findSharedDomain(domain) == null){
            throw new RuntimeException("The target shared domain " +domain + " does not exist");
        }
        return config.get(VanillaCloudfoundryApplication.APPLICATION_NAME) + "." + domain;
    }

    private CloudDomain findSharedDomain(final String domainName) {
        return Iterables.find(getClient().getSharedDomains(), new Predicate<CloudDomain>() {
            @Override
            public boolean apply(CloudDomain domain) {
                return domainName.equals(domain.getName());
            }
        }, null);
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

    private String getDomainUri(String applicationName) {
        String domainUri = null;
        Optional<CloudApplication> optional = getApplication(applicationName);
        if (optional.isPresent()) {
            domainUri = "https://" + optional.get().getUris().get(0);
        }
        return domainUri;
    }

    private Optional<CloudApplication> getApplication(String applicationName) {
        return Optional.fromNullable(getClient().getApplication(applicationName));
    }

    public void pushArtifact(String applicationName, String artifact) {
        try {
            getClient().uploadApplication(applicationName, artifact);
        } catch (IOException e) {
            log.error("Error updating the application artifact {} in {} ", artifact, this);
            throw Exceptions.propagate(e);
        }
    }

    public StartingInfo startApplication(String applicationName) {
        return getClient().startApplication(applicationName);
    }

    public void setEnv(String applicationName, Map<Object, Object> envs) {
        //TODO
        getClient().getApplication(applicationName).setEnv(envs);
    }

    public void stopApplication(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void deleteApplication(String applicationName) {
        getClient().deleteApplication(applicationName);
    }

    public void restart(String applicationName) {
        //TODO
    }

    public CloudApplication.AppState getApplicationStatus(String applicationName) {
        Optional<CloudApplication> optional = getApplication(applicationName);
        if (optional.isPresent()) {
            return optional.get().getState();
        } else {
            throw Exceptions.propagate(new CloudFoundryException(HttpStatus.NOT_FOUND));
        }
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }
}
