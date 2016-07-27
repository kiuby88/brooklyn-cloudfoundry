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
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.location.paas.PaasLocationConfig;
import org.apache.brooklyn.cloudfoundry.location.supplier.CloudFoundryClientSupplier;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.location.paas.PaasLocation;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryPaasLocation extends AbstractLocation implements PaasLocation, CloudFoundryPaasLocationConfig {

    public static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasLocation.class);

    public CloudFoundryPaasLocation(Map<?, ?> properties) {
        super(properties);
    }

    protected CloudFoundryClient getClient() throws MalformedURLException {
        return new CloudFoundryClientSupplier(
                getConfig(PaasLocationConfig.ACCESS_IDENTITY),
                getConfig(PaasLocationConfig.ACCESS_CREDENTIAL),
                URI.create(getConfig(PaasLocationConfig.CLOUD_ENDPOINT)).toURL(),
                null,
                null, // TODO getConfig(PaasLocationConfig.ORG),
                null, // TODO getConfig(PaasLocationConfig.SPACE),
                true// TODO getConfig(PaasLocationConfig.SELF_SIGNED_CERT)
        ).get();
    }

    @Override
    public String getPaasProviderName() {
        return "CloudFoundry";
    }

    public String deploy(Map<?, ?> params) {
        ConfigBag appSetUp = ConfigBag.newInstance(params);
        String artifactLocalPath = appSetUp.get(VanillaCloudFoundryApplication.ARTIFACT_PATH);
        String applicationName = appSetUp.get(VanillaCloudFoundryApplication.APPLICATION_NAME);

        getClient().createApplication(applicationName, getStaging(appSetUp),
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_DISK),
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_MEMORY),
                getUris(appSetUp), null);

        setInstancesNumber(applicationName,
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_INSTANCES));

        pushArtifact(applicationName, artifactLocalPath);
        return getDomainUri(applicationName);
    }

    public void startApplication(String applicationName) {
        getClient().startApplication(applicationName);
    }

    public void stop(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void restart(String applicationName) {
        getClient().restartApplication(applicationName);
    }

    public void delete(String applicationName) {
        getClient().deleteApplication(applicationName);
    }

    public void setEnv(String applicationName, Map<String, String> env) {
        getClient().setEnv(applicationName, env);
    }

    public Map<String, String> getEnv(String applicationName) {
        return getClient().getEnv(applicationName);
    }

    public void setInstancesNumber(String applicationName, int instancesNumber) {
        getClient().setInstancesNumber(applicationName, instancesNumber);
    }

    public void setDiskQuota(String applicationName, int diskQuota) {
        getClient().setDiskQuota(applicationName, diskQuota);
    }

    public void setMemory(String applicationName, int memory) {
        getClient().setMemory(applicationName, memory);
    }

    public int getInstancesNumber(String applicationName) {
        return getClient().getInstancesNumber(applicationName);
    }

    public int getDiskQuota(String applicationName) {
        return getClient().getDiskQuota(applicationName);
    }

    public int getMemory(String applicationName) {
        return getClient().getMemory(applicationName);
    }

}
