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

import org.apache.brooklyn.cloudfoundry.location.paas.PaasApplication;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudFoundryPaasApplication implements PaasApplication {

    private final CloudFoundryPaasLocation platform;
    private CloudFoundryClient client;

    public CloudFoundryPaasApplication(CloudFoundryPaasLocation platform) {
        this.platform = platform;
        init();
    }

    public void init() {
        setUpClient();
    }

    private void setUpClient() {
        CloudCredentials credentials = new CloudCredentials(platform.getConfig(CloudFoundryPaasLocation.CF_USER),
                platform.getConfig(CloudFoundryPaasLocation.CF_PASSWORD));
        client = new CloudFoundryClient(credentials, getTargetURL(platform.getConfig(CloudFoundryPaasLocation.CF_ENDPOINT)),
                platform.getConfig(CloudFoundryPaasLocation.CF_ORG), platform.getConfig(CloudFoundryPaasLocation.CF_SPACE), true);
    }

    public CloudFoundryPaasLocation getPlatform() {
        return platform;
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }
}
