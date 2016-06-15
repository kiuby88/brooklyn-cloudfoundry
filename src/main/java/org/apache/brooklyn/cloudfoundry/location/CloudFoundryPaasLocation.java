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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public static ConfigKey<String> CF_USER = ConfigKeys.newStringConfigKey("user");
    public static ConfigKey<String> CF_PASSWORD = ConfigKeys.newStringConfigKey("password");
    public static ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org");
    public static ConfigKey<String> CF_ENDPOINT = ConfigKeys.newStringConfigKey("endpoint");
    public static ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space");

    private CloudFoundryClient client;
    private OAuth2AccessToken accessToken;

    public CloudFoundryPaasLocation() {
        super();
    }

    public CloudFoundryPaasLocation(Map<?, ?> properties) {
        super(properties);
    }

    @Override
    public void init() {
        super.init();
        if (client == null) {
            client = new CloudFoundryClient(new CloudCredentials(getConfig(CF_USER), getConfig(CF_PASSWORD)),
                    getTargetURL(getConfig(CF_ENDPOINT)), getConfig(CF_ORG), getConfig(CF_SPACE), true);
        }
    }

    public synchronized void login() {
        if ((accessToken == null) || (accessToken.isExpired())) {
            accessToken = client.login();
        }
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
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

}