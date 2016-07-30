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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.brooklyn.cloudfoundry.location.paas.PaasLocationConfig;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;


public class CloudFoundryPaasClientRegistryImpl implements CloudFoundryClientRegistry {

    public static final CloudFoundryPaasClientRegistryImpl INSTANCE = new CloudFoundryPaasClientRegistryImpl();

    protected CloudFoundryPaasClientRegistryImpl() {
    }

    @Override
    public CloudFoundryPaasClient getCloudFoundryClient(ConfigBag conf, boolean allowReuse) {
        String username = checkNotNull(conf.get(PaasLocationConfig.ACCESS_IDENTITY), "identity must not be null");
        String password = checkNotNull(conf.get(PaasLocationConfig.ACCESS_CREDENTIAL), "credential must not be null");
        URL apiHost = getTargetURL(checkNotNull(conf.get(PaasLocationConfig.CLOUD_ENDPOINT), "endpoint must not be null"));
        String organization = checkNotNull(conf.get(CloudFoundryPaasLocationConfig.CF_ORG), "organization must not be null");
        String space = checkNotNull(conf.get(CloudFoundryPaasLocationConfig.CF_SPACE), "space must not be null");

        CloudCredentials credentials = new CloudCredentials(username, password);
        CloudFoundryClient client = new CloudFoundryClient(credentials, apiHost, organization, space, true);

        client.login();

        return getClougFoundryPaasClient(client);
    }

    private CloudFoundryPaasClient getClougFoundryPaasClient(CloudFoundryClient client) {
        return new CloudFoundryPaasClient(client);
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

}

