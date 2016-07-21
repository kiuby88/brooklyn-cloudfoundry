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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryLiveTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class VanillaCloudFoundryApplicationLiveTest extends AbstractCloudFoundryLiveTest {

    @Test(groups = {"Live"})
    public void testDeployApplication() throws IOException {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));
        startAndCheckEntitySensorsAndDefaultProfile(entity, cloudFoundryPaasLocation);

        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_ENV).isEmpty());
    }

    @Test(groups = {"Live"})
    public void testDeployApplicationWithoutDomain() throws IOException {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
    }

    @Test(groups = {"Live"})
    public void testDeployApplicationWitEnv() throws IOException {
        Map<String, String> env = MutableMap.copyOf(SIMPLE_ENV);

        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.ENV, env)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_ENV), env);
    }

    @Test(groups = {"Live"})
    @SuppressWarnings("unchecked")
    public void testSetEnvEffector() throws IOException {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_ENV).isEmpty());
        entity.setEnv("k1", "v1");
        assertFalse(entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_ENV).isEmpty());
        Map<String, String> envs =
                entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_ENV);
        assertEquals(envs, MutableMap.of("k1", "v1"));
    }

    @Test(groups = {"Live"})
    public void testModifyApplicationResources() throws IOException {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensorsAndDefaultProfile(entity, cloudFoundryPaasLocation);

        entity.setMemory(CUSTOM_MEMORY);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY).intValue(),
                CUSTOM_MEMORY);

        entity.setDiskQuota(CUSTOM_DISK);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK).intValue(),
                CUSTOM_DISK);

        entity.setInstancesNumber(CUSTOM_INSTANCES);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES).intValue(),
                CUSTOM_INSTANCES);
    }

    @Test(groups = {"Live"})
    public void testStopApplication() {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        entity.stop();
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertNull(entity.getAttribute(Startable.SERVICE_UP));
                assertNull(entity.getAttribute(VanillaCloudFoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
    }

    @Test(groups = {"Live"})
    public void testRestartApplication() throws IOException {
        final VanillaCloudFoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudFoundryApplication.class)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudFoundryApplication.BUILDPACK, JAVA_BUILDPACK));
        startAndCheckEntitySensorsAndDefaultProfile(entity, cloudFoundryPaasLocation);
        entity.restart();
        checkEntityIsRunningAndAvailable(entity);
    }

    private void startAndCheckEntitySensors(VanillaCloudFoundryApplication entity,
                                            CloudFoundryPaasLocation location) {
        app.start(ImmutableList.of(location));
        checkEntityIsRunningAndAvailable(entity);
    }

    private void checkEntityIsRunningAndAvailable(final VanillaCloudFoundryApplication entity) {
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                assertTrue(entity.getAttribute(VanillaCloudFoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
        assertFalse(Strings.isBlank(entity.getAttribute(Attributes.MAIN_URI).toString()));
        assertFalse(Strings.isBlank(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL)));

    }

    private void startAndCheckEntitySensorsAndDefaultProfile(VanillaCloudFoundryApplication entity,
                                                             CloudFoundryPaasLocation location) {
        startAndCheckEntitySensors(entity, location);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_MEMORY));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_DISK));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_INSTANCES));
    }

}
