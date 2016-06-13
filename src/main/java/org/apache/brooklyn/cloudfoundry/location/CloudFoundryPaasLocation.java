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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public static ConfigKey<String> CF_USER = ConfigKeys.newStringConfigKey("user");
    public static ConfigKey<String> CF_PASSWORD = ConfigKeys.newStringConfigKey("password");
    public static ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org");
    public static ConfigKey<String> CF_ENDPOINT = ConfigKeys.newStringConfigKey("endpoint");
    public static ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space");

    private static final String DOMAIN_KEYWORD = "-domain.";

    CloudFoundryClient client;

    public CloudFoundryPaasLocation() {
        super();
    }

    @Override
    public void init() {
        super.init();
    }

    public void setUpClient() {
        if (client == null) {
            CloudCredentials credentials =
                    new CloudCredentials(getConfig(CF_USER), getConfig(CF_PASSWORD));
            client = new CloudFoundryClient(credentials,
                    getTargetURL(getConfig(CF_ENDPOINT)),
                    getConfig(CF_ORG), getConfig(CF_SPACE), true);
            client.login();
        }
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }

    public String deploy(String applicationName, String buildpack, String localArtifactPath) {
        List<String> uris = MutableList.of();
        Staging staging;
        staging = new Staging(null, buildpack);
        uris.add(inferApplicationDomainUri(applicationName));

        getClient().createApplication(applicationName, staging,
                getConfig(CloudFoundryPaasLocation.REQUIRED_MEMORY),
                uris, null);
        pushApplication(applicationName, localArtifactPath);
        return getDomainUri(applicationName);
    }

    private void pushApplication(String applicationName, String localArtifactPath) {
        try {
            getClient().uploadApplication(applicationName, localArtifactPath);
        } catch (IOException e) {
            log.error("Error deploying application {} ", this);
            throw Exceptions.propagate(e);
        }
    }

    public String getDomainUri(String applicationName) {
        String domainUri = null;
        Optional<CloudApplication> optional = getApplication(applicationName);
        if (optional.isPresent()) {
            domainUri = "https://" + optional.get().getUris().get(0);
        }
        return domainUri;
    }

    public void startApplication(String applicationName) {
        getClient().startApplication(applicationName);
    }

    public void stopApplication(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void deleteApplication(String applicationName) {
        getClient().deleteApplication(applicationName);
    }

    private String inferApplicationDomainUri(String name) {
        String defaultDomainName = getClient().getDefaultDomain().getName();
        return name + DOMAIN_KEYWORD + defaultDomainName;
    }

    private Optional<CloudApplication> getApplication(String applicationName) {
        return Optional.fromNullable(getClient().getApplication(applicationName));
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

    public CloudFoundryClient getClient() {
        return client;
    }

    public void setClient(CloudFoundryClient client) {
        this.client = client;
    }

}