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
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation.AppState;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationTest extends AbstractCloudFoundryUnitTest {

    @SuppressWarnings("all")
    public final String APPLICATION_LOCAL_PATH = getClass()
            .getClassLoader().getResource(APPLICATION_ARTIFACT).getPath();

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testDeployApplication() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
    }

    @Test
    public void testDeployApplicationWithHost() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_HOST, BROOKLYN_HOST);
        deployApplicationAndCheck(params);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployApplicationNoNameNoHost() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_LOCAL_PATH);
        deployApplicationAndCheck(params);
    }

    @Test
    public void testDeployApplicationWithDomain() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        deployApplicationAndCheck(params);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeployApplicationWithNonExistentDomain() {
        ConfigBag params = getDefaultResourcesProfile();
        String nonExistentDomain = Strings.makeRandomId(8);
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, nonExistentDomain);
        deployApplicationAndCheck(params);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testDeployWithoutArtifact() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey(), APPLICATION_NAME);
        cloudFoundryPaasLocation.deploy(params.getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployNonExistentArtifact() {
        ConfigBag params = getDefaultApplicationConfiguration();
        String nonExistentArtifact = Strings.makeRandomId(10);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, nonExistentArtifact);
        cloudFoundryPaasLocation.deploy(params.getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testUpdateAnNotExistentArtifact() {
        String nonExistentArtifact = Strings.makeRandomId(10);
        cloudFoundryPaasLocation.pushArtifact(APPLICATION_NAME, nonExistentArtifact);
    }

    @Test
    public void testStartApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        deployApplication(params);
        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STARTED);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testStartNonExistentApplication() {
        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
    }

    @Test
    public void testGetApplicationStatus() {
        ConfigBag params = getDefaultApplicationConfiguration();
        deployApplication(params);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STOPPED);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testGetStateNonExistentApplication() {
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testStopApplication() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        deployApplication(params);

        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STARTED);

        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STOPPED);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testStopNonExistentApplication() {
        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
    }

    @Test
    public void restartApplication() {
        deployApplication(getDefaultApplicationConfiguration());

        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STARTED);

        cloudFoundryPaasLocation.restartApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                AppState.STARTED);
    }

    @Test
    public void testDeleteApplication() {
        deployApplication(getDefaultApplicationConfiguration());

        cloudFoundryPaasLocation.deleteApplication(APPLICATION_NAME);
        assertFalse(cloudFoundryPaasLocation.isDeployed(APPLICATION_NAME));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeleteNonExistentApplication() {
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testDefaultEmptyEnvConfiguration() {
        deployApplication(getDefaultApplicationConfiguration());
        assertTrue(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME).isEmpty());
    }

    @Test
    public void testAddEnvToEmptyApplication() {
        deployApplication(getDefaultApplicationConfiguration());
        testAdditionOfEnv(MutableMap.<String, String>of(), SIMPLE_ENV);
    }

    @Test
    public void testAddNullEnvToEmptyApplication() {
        deployApplication(getDefaultApplicationConfiguration());
        testAdditionOfEnv(EMPTY_ENV, null);
    }

    @Test
    public void testAddEnvToNotEmptyApplication() throws IOException {
        Map<String, String> defaultEnv = MutableMap.of("keyDefault1", "valueDefault1");
        Map<String, String> joinedEnv = MutableMap.copyOf(defaultEnv);

        deployApplication(getDefaultApplicationConfiguration());
        assertTrue(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME).isEmpty());

        testAdditionOfEnv(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME), defaultEnv);
        joinedEnv.putAll(SIMPLE_ENV);
        testAdditionOfEnv(defaultEnv, SIMPLE_ENV);
    }

    @Test
    public void testGetMemory() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), MEMORY);
    }

    @Test
    public void testSetMemory() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), MEMORY);

        cloudFoundryPaasLocation.setMemory(APPLICATION_NAME, CUSTOM_MEMORY);
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), CUSTOM_MEMORY);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testSetMemoryNonExistentApplication() {
        cloudFoundryPaasLocation.setMemory(APPLICATION_NAME, MEMORY);
    }

    @Test
    public void testGetDiskQuota() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(APPLICATION_NAME), DISK);
    }

    @Test
    public void testSetDiskQuota() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(APPLICATION_NAME), DISK);

        cloudFoundryPaasLocation.setDiskQuota(APPLICATION_NAME, CUSTOM_DISK);
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(APPLICATION_NAME), CUSTOM_DISK);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testSetDiskQuotaNonExistentApplication() {
        cloudFoundryPaasLocation.setDiskQuota(APPLICATION_NAME, DISK);
    }

    @Test
    public void testGetInstances() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(APPLICATION_NAME), INSTANCES);
    }

    @Test
    public void testSetIntances() {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(APPLICATION_NAME), INSTANCES);

        cloudFoundryPaasLocation.setInstancesNumber(APPLICATION_NAME, CUSTOM_INSTANCES);
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(APPLICATION_NAME), CUSTOM_INSTANCES);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testSetInstancesNonExistentApplication() {
        cloudFoundryPaasLocation.setInstancesNumber(APPLICATION_NAME, INSTANCES);
    }

    @Test
    public void testCreateService() {
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testRepeatInstanceNameService() {
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        cloudFoundryPaasLocation.createServiceInstance(getDefaultServiceConfig().getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInstanceOfNonExistentService() {
        ConfigBag params = getDefaultServiceConfig();
        params.configure(VanillaCloudFoundryService.SERVICE_NAME, NON_EXISTENT_SERVICE);
        cloudFoundryPaasLocation.createServiceInstance(params.getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInstanceOfNonSupportedPlan() {
        ConfigBag params = getDefaultServiceConfig();
        params.configure(VanillaCloudFoundryService.PLAN, NON_SUPPORTED_PLAN);
        cloudFoundryPaasLocation.createServiceInstance(params.getAllConfig());
    }

    @Test
    public void testBindServiceToApplication() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        assertFalse(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, APPLICATION_NAME));
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
        assertTrue(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, APPLICATION_NAME));
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testBindNonExistentServiceToApplication() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testBindServiceToNonExistentApplication() {
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
    }

    @Test
    public void testDeleteService() {
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeleteNonExistentService() {
        deleteServiceAndCheck(SERVICE_INSTANCE_NAME);
    }


    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeleteABoundService() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        assertFalse(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, APPLICATION_NAME));
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
        assertTrue(cloudFoundryPaasLocation.isServiceBoundTo(SERVICE_INSTANCE_NAME, APPLICATION_NAME));
        cloudFoundryPaasLocation.deleteServiceInstance(SERVICE_INSTANCE_NAME);
    }

    @Test
    public void testGetCredentials() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        cloudFoundryPaasLocation.bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
        Map<String, String> credentials = cloudFoundryPaasLocation
                .getCredentialsServiceForApplication(APPLICATION_NAME, SERVICE_INSTANCE_NAME);
        assertNotNull(credentials);
        assertFalse(credentials.isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetCredentialsForNotBoundService() {
        deployApplicationAndCheck(getDefaultApplicationConfiguration());
        createServiceAndCheck(getDefaultServiceConfig().getAllConfig());
        Map<String, String> credentials = cloudFoundryPaasLocation
                .getCredentialsServiceForApplication(APPLICATION_NAME, SERVICE_INSTANCE_NAME);
        assertNotNull(credentials);
    }

    private ConfigBag getDefaultApplicationConfiguration() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey(), APPLICATION_NAME);
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_LOCAL_PATH);
        params.configure(VanillaCloudFoundryApplication.BUILDPACK, MOCK_BUILDPACK);
        return params;
    }

    private ConfigBag getDefaultResourcesProfile() {
        ConfigBag params = new ConfigBag();
        params.configure(VanillaCloudFoundryApplication.REQUIRED_INSTANCES, INSTANCES);
        params.configure(VanillaCloudFoundryApplication.REQUIRED_MEMORY, MEMORY);
        params.configure(VanillaCloudFoundryApplication.REQUIRED_DISK, DISK);
        return params;
    }

    private void deployApplicationAndCheck(ConfigBag params) {
        String applicationUrl = deployApplication(params);
        assertEquals(applicationUrl, inferApplicationUrl(params));
        assertFalse(Strings.isBlank(applicationUrl));
        String applicationName = params.get(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey());
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(applicationName), AppState.STOPPED);
    }

    private String deployApplication(ConfigBag params) {
        return cloudFoundryPaasLocation.deploy(params.getAllConfig());
    }

    private void testAdditionOfEnv(Map<String, String> applicationEnv,
                                   Map<String, String> envToAdd) {
        boolean addNullEnv = (envToAdd == null);
        Map<String, String> joinedEnv = MutableMap.copyOf(applicationEnv);
        if (!addNullEnv) {
            joinedEnv.putAll(envToAdd);
        }
        assertEquals(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME), applicationEnv);
        cloudFoundryPaasLocation.setEnv(APPLICATION_NAME, envToAdd);
        Map<String, String> returnedEnv = cloudFoundryPaasLocation.getEnv(APPLICATION_NAME);
        assertEquals(returnedEnv, joinedEnv);
    }

}
