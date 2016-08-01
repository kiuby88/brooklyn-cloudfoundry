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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationTest extends AbstractCloudFoundryUnitTest {

    private CloudFoundryOperations client;

    @SuppressWarnings("all")
    public final String APPLICATION_LOCAL_PATH = getClass()
            .getClassLoader().getResource(APPLICATION_ARTIFACT).getPath();

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        client = cloudFoundryPaasLocation.getClient();
    }

    @Test
    public void testDeployApplicationDomain() throws IOException {
        testDeployApplication(getDefaultApplicationConfiguration());
        verify(client, times(1)).getDefaultDomain();
    }

    @Test
    public void testDeployApplicationWithDomain() throws IOException {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        testDeployApplication(params);
        verify(client, never()).getDefaultDomain();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeployApplicationWithNonExistentDomain() throws IOException {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, Strings.makeRandomId(8));
        testDeployApplication(params);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployNonExistentArtifact() throws IOException {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, Strings.makeRandomId(10));
        cloudFoundryPaasLocation.deploy(params.getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testUpdateAnNotExistentArtifact() throws IOException {
        cloudFoundryPaasLocation.pushArtifact(APPLICATION_NAME, Strings.makeRandomId(10));
    }

    @Test
    public void testStartApplication() throws IOException {
        ConfigBag params = getDefaultApplicationConfiguration();
        deployApplication(params);

        StartingInfo startingInfo = cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertNotNull(startingInfo);
        assertEquals(startingInfo.getStagingFile(), Strings.EMPTY);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STARTED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStartNonExistentApplication() {
        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
    }

    @Test
    public void testGetApplicationStatus() throws IOException {
        ConfigBag params = getDefaultApplicationConfiguration();
        deployApplication(params);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STOPPED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testGetStateNonExistentApplication() {
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testInferApplicationRouteUriNoHost() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        assertEquals(cloudFoundryPaasLocation.inferApplicationRouteUri(params),
                APPLICATION_NAME + "." + BROOKLYN_DOMAIN);
    }

    @Test
    public void testInferApplicationRouteUriWithHost() {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        params.configure(VanillaCloudFoundryApplication.APPLICATION_HOST, MOCK_HOST);

        assertEquals(cloudFoundryPaasLocation.inferApplicationRouteUri(params),
                MOCK_HOST + "." + BROOKLYN_DOMAIN);
    }

    @Test
    public void testInferApplicationRouteUriNoDomain() {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey(), APPLICATION_NAME);

        assertEquals(cloudFoundryPaasLocation.inferApplicationRouteUri(params),
                APPLICATION_NAME + "." + BROOKLYN_DOMAIN);
    }

    @Test
    public void testStopApplication() throws IOException {
        ConfigBag params = getDefaultApplicationConfiguration();
        params.configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN);
        deployApplication(params);

        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STARTED);

        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STOPPED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStopNonExistentApplication() {
        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
    }

    @Test
    public void restartApplication() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());

        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STARTED);

        cloudFoundryPaasLocation.restartApplication(APPLICATION_NAME);
        assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME), AppState.STARTED);
    }

    @Test
    public void testDeleteApplication() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());

        cloudFoundryPaasLocation.deleteApplication(APPLICATION_NAME);
        assertFalse(cloudFoundryPaasLocation.isDeployed(APPLICATION_NAME));
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testDeleteNonExistentApplication() {
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testDefaultEmptyEnvConfiguration() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertTrue(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME).isEmpty());
    }

    @Test
    public void testAddEnvToEmptyApplication() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());

        testAdditionOfEnv(MutableMap.<String, String>of(), SIMPLE_ENV);
        verify(client, times(1)).updateApplicationEnv(APPLICATION_NAME, SIMPLE_ENV);
    }

    @Test
    public void testAddNullEnvToEmptyApplication() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());

        testAdditionOfEnv(EMPTY_ENV, null);
        verify(client, never()).updateApplicationEnv(APPLICATION_NAME, EMPTY_ENV);
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

        verify(client, times(1)).updateApplicationEnv(APPLICATION_NAME, joinedEnv);
    }

    @Test
    public void testSetMemory() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), MEMORY);

        cloudFoundryPaasLocation.setMemory(APPLICATION_NAME, CUSTOM_MEMORY);
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), CUSTOM_MEMORY);
        verify(client, times(1)).updateApplicationMemory(APPLICATION_NAME, CUSTOM_MEMORY);
    }

    @Test
    public void testGetMemory() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getMemory(APPLICATION_NAME), MEMORY);
    }

    @Test
    public void testSetDiskQuota() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(APPLICATION_NAME), DISK);

        cloudFoundryPaasLocation.setDiskQuota(APPLICATION_NAME, CUSTOM_DISK);
        verify(client, times(1)).updateApplicationDiskQuota(APPLICATION_NAME, CUSTOM_DISK);
    }

    @Test
    public void testGetDiskQuota() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getDiskQuota(APPLICATION_NAME), DISK);
    }

    @Test
    public void testSetIntances() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(APPLICATION_NAME), INSTANCES);

        cloudFoundryPaasLocation.setInstancesNumber(APPLICATION_NAME, CUSTOM_INSTANCES);
        verify(client, times(1)).updateApplicationInstances(APPLICATION_NAME, CUSTOM_INSTANCES);
    }

    @Test
    public void testGetInstances() throws IOException {
        deployApplication(getDefaultApplicationConfiguration());
        assertEquals(cloudFoundryPaasLocation.getInstancesNumber(APPLICATION_NAME), INSTANCES);
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

    private void testDeployApplication(ConfigBag params) throws IOException {
        String applicationDomain = deployApplication(params);
        String applicationName = params.get(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey());
        assertEquals(applicationDomain, DEFAULT_APPLICATION_ADDRESS);
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
