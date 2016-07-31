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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryLiveTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CloudFoundryPaasClientLiveTest extends AbstractCloudFoundryLiveTest {

    private CloudFoundryPaasClient cloudFoundryPaasClient;
    private String applicationName;
    private String artifactLocalPath;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        cloudFoundryPaasClient = cloudFoundryPaasLocation.getConfig(CloudFoundryPaasLocation.CF_CLIENT_REGISTRY)
                .getCloudFoundryClient(cloudFoundryPaasLocation.config().getBag(), false);

        applicationName = APPLICATION_NAME_PREFIX + UUID.randomUUID()
                .toString().substring(0, 8);
        artifactLocalPath = getLocalPath(APPLICATION_ARTIFACT);
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagement() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagementWithDomain() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testAddEnvToApplication() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        String applicationUrl = cloudFoundryPaasClient.deploy(params.getAllConfig());
        startApplication(applicationName, applicationUrl);

        Map<String, String> env = cloudFoundryPaasClient.getEnv(applicationName);
        assertTrue(env.isEmpty());

        cloudFoundryPaasClient.setEnv(applicationName, SIMPLE_ENV);
        env = cloudFoundryPaasClient.getEnv(applicationName);
        assertEquals(env, SIMPLE_ENV);
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testAddNullEnvToApplication() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        String applicationUrl = cloudFoundryPaasClient.deploy(params.getAllConfig());
        startApplication(applicationName, applicationUrl);

        Map<String, String> env = cloudFoundryPaasClient.getEnv(applicationName);
        assertTrue(env.isEmpty());

        cloudFoundryPaasClient.setEnv(applicationName, null);
        env = cloudFoundryPaasClient.getEnv(applicationName);
        assertTrue(env.isEmpty());
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testModifyResourcesForApplication() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        String applicationUrl = cloudFoundryPaasClient.deploy(params.getAllConfig());
        assertFalse(Strings.isBlank(applicationUrl));

        startApplication(applicationName, applicationUrl);
        assertEquals(cloudFoundryPaasClient.getMemory(applicationName), MEMORY);
        assertEquals(cloudFoundryPaasClient.getDiskQuota(applicationName), DISK);
        assertEquals(cloudFoundryPaasClient.getInstancesNumber(applicationName), INSTANCES);

        cloudFoundryPaasClient.setMemory(applicationName, CUSTOM_MEMORY);
        cloudFoundryPaasClient.setDiskQuota(applicationName, CUSTOM_DISK);
        cloudFoundryPaasClient.setInstancesNumber(applicationName, CUSTOM_INSTANCES);

        assertEquals(cloudFoundryPaasClient.getMemory(applicationName), CUSTOM_MEMORY);
        assertEquals(cloudFoundryPaasClient.getDiskQuota(applicationName), CUSTOM_DISK);
        assertEquals(cloudFoundryPaasClient.getInstancesNumber(applicationName), CUSTOM_INSTANCES);
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testRestartApplication() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        String applicationUrl = cloudFoundryPaasClient.deploy(params.getAllConfig());
        assertFalse(Strings.isBlank(applicationUrl));
        startApplication(applicationName, applicationUrl);

        cloudFoundryPaasClient.restartApplication(applicationName);
        checkDeployedApplicationAvailability(applicationName, applicationUrl);
        destroyApplication(applicationName, applicationUrl);
    }

    private void applicationLifecycleManagement(String applicationName, Map<String, Object> params) {
        String applicationUrl = cloudFoundryPaasClient.deploy(params);
        assertFalse(Strings.isBlank(applicationUrl));
        startApplication(applicationName, applicationUrl);
        destroyApplication(applicationName, applicationUrl);
    }

    private void startApplication(String applicationName, String applicationDomain) {
        cloudFoundryPaasClient.startApplication(applicationName);
        checkDeployedApplicationAvailability(applicationName, applicationDomain);
    }

    private void checkDeployedApplicationAvailability(final String applicationName,
                                                      final String applicationDomain) {
        Map<String, ?> flags = ImmutableMap.of("timeout", Duration.TWO_MINUTES);
        Asserts.succeedsEventually(flags, new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain), HttpURLConnection.HTTP_OK);
                    assertEquals(cloudFoundryPaasClient.getApplicationStatus(applicationName),
                            CloudApplication.AppState.STARTED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void destroyApplication(String applicationName, String applicationUrl) {
        stopApplication(applicationName, applicationUrl);
        deleteApplicatin(applicationName);
    }

    private void stopApplication(final String applicationName, final String applicationDomain) {
        cloudFoundryPaasClient.stopApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain),
                            HttpURLConnection.HTTP_NOT_FOUND);
                    assertEquals(cloudFoundryPaasClient.getApplicationStatus(applicationName),
                            CloudApplication.AppState.STOPPED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void deleteApplicatin(final String applicationName) {
        cloudFoundryPaasClient.deleteApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertFalse(cloudFoundryPaasClient.isDeployed(applicationName));
            }
        });
    }

    private ConfigBag getDefaultResourcesProfile() {
        ConfigBag params = new ConfigBag();
        params.configure(VanillaCloudFoundryApplication.REQUIRED_INSTANCES, INSTANCES);
        params.configure(VanillaCloudFoundryApplication.REQUIRED_MEMORY, MEMORY);
        params.configure(VanillaCloudFoundryApplication.REQUIRED_DISK, DISK);
        return params;
    }

    @SuppressWarnings("all")
    public String getLocalPath(String filename) {
        try {
            return Paths.get(getClass().getClassLoader().getResource(filename).toURI()).toString();
        } catch (URISyntaxException e) {
            return "";
        }
    }


}
