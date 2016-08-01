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
package org.apache.brooklyn.cloudfoundry.entity;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.FakeCloudFoundryClient;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.util.collections.MutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VanillaPaasApplicationCloudFoundryDriverIntegrationTest extends AbstractCloudFoundryUnitTest {

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation(true);
    }

    @Test
    public void testStartApplication() throws MalformedURLException {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        testSensorInitialization(entity);
        assertTrue(driver.isRunning());
        assertTrue(EMPTY_ENV.isEmpty());
    }

    @Test
    public void testStartApplicationWithEnv() {
        VanillaCloudFoundryApplicationImpl entity = createEntity(SIMPLE_ENV);
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());
        testSensorInitialization(entity);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), SIMPLE_ENV);
    }

    @Test
    public void testSetEnvToApplication() {
        Map<String, String> env = MutableMap.copyOf(SIMPLE_ENV);
        VanillaCloudFoundryApplicationImpl entity = createEntity(SIMPLE_ENV);
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), env);
        assertTrue(driver.isRunning());

        Map<String, String> newEnv = MutableMap.of("k2", "v2");
        driver.setEnv(newEnv);
        env.putAll(newEnv);
        entity.getAttribute(VanillaCloudFoundryApplication.ENV);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), env);
    }

    @Test
    @SuppressWarnings("all")
    public void testSetNullEnvToApplication() {
        Map<String, String> newEnv = null;
        VanillaCloudFoundryApplicationImpl entity = createEntity(SIMPLE_ENV);
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        driver.setEnv(newEnv);
        assertEquals(entity
                .getAttribute(VanillaCloudFoundryApplication.ENV), SIMPLE_ENV);
        assertTrue(driver.isRunning());
    }

    @Test
    public void testSetMemory() {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());
        checkDefaultResourceProfile(entity);

        driver.setMemory(CUSTOM_MEMORY);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY).intValue(),
                CUSTOM_MEMORY);
    }

    @Test
    public void testSetDisk() {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());
        checkDefaultResourceProfile(entity);

        driver.setDiskQuota(CUSTOM_DISK);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK).intValue(),
                CUSTOM_DISK);
    }

    @Test
    public void testSetInstances() {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());
        checkDefaultResourceProfile(entity);

        driver.setInstancesNumber(CUSTOM_INSTANCES);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES).intValue(),
                CUSTOM_INSTANCES);
    }

    @Test
    public void testStopApplication() throws IOException {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());

        driver.stop();
        assertFalse(driver.isRunning());
    }

    @Test
    public void testRestartApplication() {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());

        driver.restart();
    }

    @Test
    public void testDeleteApplication() throws IOException {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, cloudFoundryPaasLocation);
        driver.start();
        assertTrue(driver.isRunning());
        driver.delete();
        assertFalse(driver.isRunning());
    }

    private VanillaCloudFoundryApplicationImpl createEntity() {
        VanillaCloudFoundryApplicationImpl entity = new VanillaCloudFoundryApplicationImpl();
        entity.setConfigEvenIfOwned(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_LOCAL_PATH);
        entity.setManagementContext(mgmt);
        return entity;
    }

    private VanillaCloudFoundryApplicationImpl createEntity(Map<String, String> env) {
        VanillaCloudFoundryApplicationImpl entity = createEntity();
        entity.setConfigEvenIfOwned(VanillaCloudFoundryApplication.ENV, env);
        return entity;
    }

    private void testSensorInitialization(VanillaCloudFoundryApplicationImpl entity) {
        String appPath = applicationPathFromName(entity.getApplicationName());
        assertTrue(entity.getAttribute(Attributes.MAIN_URI).toString().endsWith(appPath));
        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL).endsWith(appPath));
        checkDefaultResourceProfile(entity);
    }

    private String applicationPathFromName(String applicationName) {
        return applicationName + "." + FakeCloudFoundryClient.BROOKLYN_DOMAIN;
    }

}
