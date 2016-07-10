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

import java.util.Map;

import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryPaasLocation extends AbstractLocation
        implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    private CloudFoundryPaasClient client;

    public CloudFoundryPaasLocation() {
        client = new CloudFoundryPaasClient(this);
    }

    @Override
    public void init() {
        super.init();
    }

    protected CloudFoundryPaasClient getClient() {
        return client;
    }

    @Override
    public String getPaasProviderName() {
        return "cloudfoundry";
    }

    public String deploy(Map<?, ?> params) {
        return getClient().deploy(params);
    }

    public void configureEnv(String applicationName, Map<Object, Object> envs) {
        getClient().setEnv(applicationName, (Map<Object, Object>) envs);
    }

    public void startApplication(String applicationName) {
        getClient().startApplication(applicationName);
    }

    public void stop(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void delete(String applicationName) {
        getClient().deleteApplication(applicationName);
    }
}
