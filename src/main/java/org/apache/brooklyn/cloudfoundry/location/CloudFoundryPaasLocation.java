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

import java.util.List;

import org.apache.brooklyn.cloudfoundry.location.paas.DeploymentPaasApplicationLocation;
import org.apache.brooklyn.cloudfoundry.location.paas.PaasApplication;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements DeploymentPaasApplicationLocation {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public static ConfigKey<String> CF_USER = ConfigKeys.newStringConfigKey("user");
    public static ConfigKey<String> CF_PASSWORD = ConfigKeys.newStringConfigKey("password");
    public static ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org");
    public static ConfigKey<String> CF_ENDPOINT = ConfigKeys.newStringConfigKey("endpoint");
    public static ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space");

    private List<CloudFoundryPaasApplication> deployedApplications;

    public void init() {
        deployedApplications = ImmutableList.of();
    }

    @Override
    public PaasApplication deploy() {
        CloudFoundryPaasApplication application = createApplication();
        deployedApplications.add(application);
        return application;
    }

    protected CloudFoundryPaasApplication createApplication() {
        return new CloudFoundryPaasApplication(this);
    }

    @Override
    public void undeploy(PaasApplication application) {
        //TODO
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }
}
