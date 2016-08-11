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
package org.apache.brooklyn.cloudfoundry.entity.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.camp.brooklyn.BrooklynCampConstants;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.launcher.SimpleYamlLauncherForTests;
import org.apache.brooklyn.launcher.camp.SimpleYamlLauncher;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VanillaCloudFoundryServiceYamlLiveTest {

    private static final String DEFAULT_ID = "my-service";
    private static final String MY_CLEARDB_INSTANCE = "my-cleardb-instance";


    private SimpleYamlLauncher launcher;

    @BeforeMethod
    public void setUp() {
        launcher = new SimpleYamlLauncherForTests();
        launcher.setShutdownAppsOnExit(true);
    }

    @AfterMethod
    public void tearDown() {
        launcher.destroyAll();
    }

    @Test(groups = {"Live"})
    public void deploySimpleClearDbService() {
        Application app = launcher
                .launchAppYaml("vanilla-cf-service-standalone.yml")
                .getApplication();

        VanillaCloudFoundryService service = (VanillaCloudFoundryService)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testRunningSensors(service);
        assertTrue(Strings
                .isNonBlank(service.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID)));
    }

    @Test(groups = {"Live"})
    public void deploySimpleClearDbServiceWithInstanceName() {
        Application app = launcher
                .launchAppYaml("vanilla-cf-service-with-name.yml")
                .getApplication();

        VanillaCloudFoundryService service = (VanillaCloudFoundryService)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testRunningSensors(service);
        assertEquals(service.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID),
                MY_CLEARDB_INSTANCE);
    }

    private void testRunningSensors(final VanillaCloudFoundryService entity) {
        Asserts.succeedsEventually(
                new Runnable() {
                    public void run() {
                        assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication
                                .SERVICE_PROCESS_IS_RUNNING));
                    }
                });
    }

    private Entity findChildEntitySpecByPlanId(Application app, String planId) {
        for (Entity child : app.getChildren()) {
            String childPlanId = child.getConfig(BrooklynCampConstants.PLAN_ID);
            if ((childPlanId != null) && (childPlanId.equals(planId))) {
                return child;
            }
        }
        return null;
    }

}
