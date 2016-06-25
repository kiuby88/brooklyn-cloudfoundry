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
import java.util.Set;

import org.apache.brooklyn.cloudfoundry.location.paas.DeployingPaasApplicationLocation;
import org.apache.brooklyn.cloudfoundry.location.paas.PaasApplication;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.util.collections.MutableSet;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements DeployingPaasApplicationLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    private Set<CloudFoundryPaasApplication> deployedApplications;

    public void init() {
        deployedApplications = MutableSet.of();
    }

    @Override
    public CloudFoundryPaasApplication deploy() {
        CloudFoundryPaasApplication application = createApplication();
        deployedApplications.add(application);
        return application;
    }

    protected CloudFoundryPaasApplication createApplication() {
        return new CloudFoundryPaasApplicationImpl(getClient());
    }

    @Override
    public void undeploy(PaasApplication application) {
        if (deployedApplications.contains(application)) {
            deployedApplications.remove(application);
        }
    }

    @Override
    public Set<CloudFoundryPaasApplication> getDeployedApplications() {
        return deployedApplications;
    }

    @Override
    public String getPaasProviderName() {
        return "cloudfoundry";
    }

    private CloudFoundryClient getClient() {
        CloudCredentials credentials = new CloudCredentials(getConfig(CloudFoundryPaasLocation.ACCESS_IDENTITY),
                getConfig(CloudFoundryPaasLocation.ACCESS_CREDENTIAL));
        return new CloudFoundryClient(credentials, getTargetURL(getConfig(CloudFoundryPaasLocation.CLOUD_ENDPOINT)),
                getConfig(CloudFoundryPaasLocation.CF_ORG), getConfig(CloudFoundryPaasLocation.CF_SPACE), true);
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }


}
