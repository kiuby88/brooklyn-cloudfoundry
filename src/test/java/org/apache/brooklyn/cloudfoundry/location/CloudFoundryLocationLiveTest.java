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
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryService;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CloudFoundryLocationLiveTest extends AbstractCloudFoundryLiveTest {

    private String applicationName;
    private String artifactLocalPath;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        applicationName = APPLICATION_NAME_PREFIX + UUID.randomUUID()
                .toString().substring(0, 8);
        artifactLocalPath = getLocalPath(APPLICATION_ARTIFACT);
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagement() {
        ConfigBag params = getDefaultApplicationConfiguration();
        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagementWithHost() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_HOST, BROOKLYN_HOST);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testWebApplicationManagementNoNameNoHost() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagementWithDomain() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"}, expectedExceptions = NullPointerException.class)
    public void testWebApplicationManagementNoArtifact() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey(), applicationName);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testAddEnvToApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        String applicationUrl = cloudFoundryPaasLocation.deploy(params.getAllConfig());
        startApplication(applicationName, applicationUrl);

        Map<String, String> env = cloudFoundryPaasLocation.getEnv(applicationName);
        assertTrue(env.isEmpty());

        cloudFoundryPaasLocation.setEnv(applicationName, SIMPLE_ENV);
        env = cloudFoundryPaasLocation.getEnv(applicationName);
        assertEquals(env, SIMPLE_ENV);
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testAddNullEnvToApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        String applicationUrl = cloudFoundryPaasLocation.deploy(params.getAllConfig());
        startApplication(applicationName, applicationUrl);

        Map<String, String> env = cloudFoundryPaasLocation.getEnv(applicationName);
        assertTrue(env.isEmpty());

        cloudFoundryPaasLocation.setEnv(applicationName, null);
        env = cloudFoundryPaasLocation.getEnv(applicationName);
        assertTrue(env.isEmpty());
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testModifyResourcesForApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        String applicationUrl = cloudFoundryPaasLocation.deploy(params.getAllConfig());
        assertFalse(Strings.isBlank(applicationUrl));

        startApplication(applicationName, applicationUrl);
        assertEquals(cloudFoundryPaasLocation.getMemory(applicationName), MEMORY);
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(applicationName), DISK);
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(applicationName), INSTANCES);

        cloudFoundryPaasLocation.setMemory(applicationName, CUSTOM_MEMORY);
        cloudFoundryPaasLocation.setDiskQuota(applicationName, CUSTOM_DISK);
        cloudFoundryPaasLocation.setInstancesNumber(applicationName, CUSTOM_INSTANCES);

        assertEquals(cloudFoundryPaasLocation.getMemory(applicationName), CUSTOM_MEMORY);
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(applicationName), CUSTOM_DISK);
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(applicationName), CUSTOM_INSTANCES);
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testSetMemoryNonExistentApplication() {
        cloudFoundryPaasLocation.setMemory(applicationName, MEMORY);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testSetDiskQuotaNonExistentApplication() {
        cloudFoundryPaasLocation.setDiskQuota(applicationName, DISK);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testSetInstancesNonExistentApplication() {
        cloudFoundryPaasLocation.setInstancesNumber(applicationName, INSTANCES);
    }

    @Test(groups = {"Live"})
    public void testRestartApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();

        String applicationUrl = cloudFoundryPaasLocation.deploy(params.getAllConfig());
        assertFalse(Strings.isBlank(applicationUrl));
        startApplication(applicationName, applicationUrl);

        cloudFoundryPaasLocation.restartApplication(applicationName);
        checkDeployedApplicationAvailability(applicationName, applicationUrl);
        destroyApplication(applicationName, applicationUrl);
    }

    @Test(groups = {"Live"})
    public void testCreateService() {
        createServiceAndCheck(getDefaultClearDbServiceConfigMap());
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testRepeatInstanceNameService() {
        createServiceAndCheck(getDefaultClearDbServiceConfigMap());
        try {
            cloudFoundryPaasLocation.createServiceInstance(getDefaultClearDbServiceConfigMap());
        } finally {
            deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
        }
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInstanceOfNonExistentService() {
        ConfigBag params = getDefaultClearDbServiceConfig();
        params.configure(VanillaCloudFoundryService.SERVICE_NAME, NON_EXISTENT_SERVICE);
        cloudFoundryPaasLocation.createServiceInstance(params.getAllConfig());
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInstanceOfNonSupportedPlan() {
        ConfigBag params = getDefaultClearDbServiceConfig();
        params.configure(VanillaCloudFoundryService.PLAN, NON_SUPPORTED_PLAN);
        cloudFoundryPaasLocation.createServiceInstance(params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testBindServiceToApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        createApplicationStartAndCheck(params.getAllConfig());
        createServiceAndCheck(getDefaultClearDbServiceConfigMap());
        assertFalse(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, applicationName));
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, applicationName);
        assertTrue(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, applicationName));
        deleteApplicatin(applicationName);
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testBindNonExistentServiceToApplication() {
        try {
            ConfigBag params = getDefaultApplicationConfiguration();
            createApplicationStartAndCheck(params.getAllConfig());
            cloudFoundryPaasLocation
                    .bindServiceToApplication(SERVICE_INSTANCE_NAME, applicationName);
        } finally {
            deleteApplicatin(applicationName);
        }
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testBindServiceToNonExistentApplication() {
        try {
            createServiceAndCheck(getDefaultClearDbServiceConfigMap());
            assertFalse(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, applicationName));
            cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, applicationName);
        } finally {
            cloudFoundryPaasLocation.deleteServiceInstance(SERVICE_INSTANCE_NAME);
        }
    }

    @Test(groups = {"Live"})
    public void testDeleteService() {
        createServiceAndCheck(getDefaultClearDbServiceConfigMap());
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testDeleteNonExistentService() {
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }

    @Test(groups = {"Live"}, expectedExceptions = PropagatedRuntimeException.class)
    public void testDeleteABoundService() {
        ConfigBag params = getDefaultApplicationConfiguration();
        createApplicationStartAndCheck(params.getAllConfig());
        createServiceAndCheck(getDefaultClearDbServiceConfigMap());
        assertFalse(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, applicationName));
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, applicationName);
        assertTrue(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, applicationName));
        try {
            cloudFoundryPaasLocation.deleteServiceInstance(SERVICE_INSTANCE_NAME);
        } finally {
            deleteApplicatin(applicationName);
            cloudFoundryPaasLocation.deleteServiceInstance(SERVICE_INSTANCE_NAME);
        }
    }

    private void applicationLifecycleManagement(String applicationName, Map<String, Object> params) {
        String applicationUrl = createApplicationStartAndCheck(params);
        destroyApplication(applicationName, applicationUrl);
    }

    private String createApplicationStartAndCheck(Map<String, Object> params) {
        String applicationUrl = cloudFoundryPaasLocation.deploy(params);
        assertEquals(applicationUrl, inferApplicationUrl(params));
        assertFalse(Strings.isBlank(applicationUrl));
        startApplication(applicationName, applicationUrl);
        return applicationUrl;
    }

    private void startApplication(String applicationName, String applicationUrl) {
        cloudFoundryPaasLocation.startApplication(applicationName);
        checkDeployedApplicationAvailability(applicationName, applicationUrl);
    }

    private void checkDeployedApplicationAvailability(final String applicationName,
                                                      final String applicationUrl) {
        Map<String, ?> flags = ImmutableMap.of("timeout", Duration.TWO_MINUTES);
        Asserts.succeedsEventually(flags, new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationUrl), HttpURLConnection.HTTP_OK);
                    assertEquals(cloudFoundryPaasLocation.getApplicationStatus(applicationName),
                            CloudFoundryPaasLocation.AppState.STARTED);
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
        cloudFoundryPaasLocation.stopApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain),
                            HttpURLConnection.HTTP_NOT_FOUND);
                    assertEquals(cloudFoundryPaasLocation.getApplicationStatus(applicationName),
                            CloudFoundryPaasLocation.AppState.STOPPED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void deleteApplicatin(final String applicationName) {
        cloudFoundryPaasLocation.deleteApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertFalse(cloudFoundryPaasLocation.isDeployed(applicationName));
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

    private ConfigBag getDefaultApplicationConfiguration() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey(), applicationName);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, artifactLocalPath);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK);
        return params;
    }

    private Map<String, Object> getDefaultClearDbServiceConfigMap() {
        return getDefaultClearDbServiceConfig().getAllConfig();
    }

    @SuppressWarnings("all")
    public String getLocalPath(String filename) {
        try {
            return Paths.get(getClass().getClassLoader().getResource(filename).toURI()).toString();
        } catch (URISyntaxException e) {
            return Strings.EMPTY;
        }
    }

}
