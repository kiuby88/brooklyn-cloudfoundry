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

import org.apache.brooklyn.cloudfoundry.location.paas.PaasLocationConfig;
import org.apache.brooklyn.location.jclouds.ComputeServiceRegistryImpl;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Throwables;

public class CloudFoundryPaasClientRegistryImpl implements CloudFoundryClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ComputeServiceRegistryImpl.class);

    public static final CloudFoundryPaasClientRegistryImpl INSTANCE = new CloudFoundryPaasClientRegistryImpl();

    protected CloudFoundryPaasClientRegistryImpl() {
    }

    @Override
    public CloudFoundryClient getCloudFoundryClient(ConfigBag conf, boolean allowReuse) {

        String provider = checkNotNull(conf.get(PaasLocationConfig.CLOUD_PROVIDER), "provider must not be null");
        String identity = checkNotNull(conf.get(PaasLocationConfig.ACCESS_IDENTITY), "identity must not be null");
        String credential = checkNotNull(conf.get(PaasLocationConfig.ACCESS_CREDENTIAL), "credential must not be null");
        String endpoint = checkNotNull(conf.get(PaasLocationConfig.CLOUD_ENDPOINT), "endpoint must not be null");

        switch (provider) {
            case "cloudfoundry":
                // TODO use complete constructor
                CloudCredentials credentials = new CloudCredentials(identity, credential);
                try {
                    return new CloudFoundryClient(credentials, URI.create(endpoint).toURL());
                } catch (MalformedURLException e) {
                    throw Throwables.propagate(e);
                }
            default:
                throw new IllegalStateException("Unexpected scope "+ provider);
        }
    }
}
