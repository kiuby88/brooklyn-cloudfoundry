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
        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));
        startAndCheckEntitySensorsAndDefaultProfile(entity, cloudFoundryPaasLocation);

        assertTrue(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV).isEmpty());
    }

    @Test(groups = {"Live"})
    public void testDeployApplicationWithoutDomain() throws IOException {
        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
    }

    @Test(groups = {"Live"})
    public void testDeployApplicationWitEnv() throws IOException {
        Map<String, String> env = MutableMap.copyOf(SIMPLE_ENV);

        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudfoundryApplication.ENV, env)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV), env);
    }

    @Test(groups = {"Live"})
    @SuppressWarnings("unchecked")
    public void testSetEnvEffector() throws IOException {
        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        assertTrue(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV).isEmpty());
        entity.setEnv("k1", "v1");
        assertFalse(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV).isEmpty());
        Map<String, String> envs =
                entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV);
        assertEquals(envs, MutableMap.of("k1", "v1"));
    }

    @Test(groups = {"Live"})
    public void testModifyApplicationResources() throws IOException {
        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensorsAndDefaultProfile(entity, cloudFoundryPaasLocation);

        entity.setMemory(CUSTOM_MEMORY);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_MEMORY).intValue(),
                CUSTOM_MEMORY);

        entity.setDiskQuota(CUSTOM_DISK);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_DISK).intValue(),
                CUSTOM_DISK);

        entity.setInstancesNumber(CUSTOM_INSTANCES);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.INSTANCES).intValue(),
                CUSTOM_INSTANCES);
    }

    @Test(groups = {"Live"})
    public void testStopApplication() {
        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK));

        startAndCheckEntitySensors(entity, cloudFoundryPaasLocation);
        entity.stop();
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertNull(entity.getAttribute(Startable.SERVICE_UP));
                assertNull(entity.getAttribute(VanillaCloudfoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
    }


    private void startAndCheckEntitySensors(final VanillaCloudfoundryApplication entity,
                                            CloudFoundryPaasLocation location) {
        app.start(ImmutableList.of(location));
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                assertTrue(entity.getAttribute(VanillaCloudfoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
        assertFalse(Strings.isBlank(entity.getAttribute(Attributes.MAIN_URI).toString()));
        assertFalse(Strings.isBlank(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL)));
    }

    private void startAndCheckEntitySensorsAndDefaultProfile(VanillaCloudfoundryApplication entity,
                                                             CloudFoundryPaasLocation location) {
        startAndCheckEntitySensors(entity, location);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_MEMORY),
                entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_MEMORY));
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_DISK),
                entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_DISK));
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.INSTANCES),
                entity.getConfig(VanillaCloudfoundryApplication.REQUIRED_INSTANCES));
    }

}
