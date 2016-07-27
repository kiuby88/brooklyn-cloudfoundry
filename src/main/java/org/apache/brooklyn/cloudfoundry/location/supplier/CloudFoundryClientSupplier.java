/**
 * Copyright 2014 SeaClouds
 * Contact: SeaClouds
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.brooklyn.cloudfoundry.location.supplier;

import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;

import com.google.common.base.Supplier;

public class CloudFoundryClientSupplier implements Supplier<CloudFoundryClient> {

    private final CloudCredentials credentials;
    private final URL endpoint;
    private final String orgName;
    private final String spaceName;
    private final HttpProxyConfiguration httpProxyConfiguration;
    private final boolean trustSelfSignedCerts;

    public CloudFoundryClientSupplier(CloudCredentials credentials, URL endpoint, String orgName, String spaceName, HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this.credentials = credentials;
        this.endpoint = endpoint;
        this.orgName = orgName;
        this.spaceName = spaceName;
        this.httpProxyConfiguration = httpProxyConfiguration;
        this.trustSelfSignedCerts = trustSelfSignedCerts;
    }

    public CloudFoundryClientSupplier(String email, String password, URL endpoint, String orgName, String spaceName, HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this.credentials = new CloudCredentials(email, password);
        this.endpoint = endpoint;
        this.orgName = orgName;
        this.spaceName = spaceName;
        this.httpProxyConfiguration = httpProxyConfiguration;
        this.trustSelfSignedCerts = trustSelfSignedCerts;
    }

    @Override
    public CloudFoundryClient get() {
        return new CloudFoundryClient(credentials, endpoint, orgName, spaceName, httpProxyConfiguration, trustSelfSignedCerts);
    }
}
