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
package org.apache.brooklyn.cloudfoundry;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.camp.brooklyn.BrooklynCampConstants;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryEntity;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.launcher.SimpleYamlLauncherForTests;
import org.apache.brooklyn.launcher.camp.SimpleYamlLauncher;
import org.apache.brooklyn.test.Asserts;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.common.collect.Iterables;

public class AbstractCloudFoundryYamlLiveTest {

    protected static final String DEFAULT_APP_ID = "vanilla-app";
    protected static final String DEFAULT_SERVICE_ID = "my-service";
    protected static final String DEFAULT_DOMAIN = "cfapps.io";
    protected static final String MY_CLEARDB_INSTANCE = "my-cleardb-instance";

    protected SimpleYamlLauncher launcher;

    @BeforeMethod
    public void setUp() {
        launcher = new SimpleYamlLauncherForTests();
        launcher.setShutdownAppsOnExit(true);
    }

    @AfterMethod
    public void tearDown() {
        launcher.destroyAll();
    }

    protected Entity findChildEntitySpecByPlanId(Application app, String planId) {
        for (Entity child : app.getChildren()) {
            String childPlanId = child.getConfig(BrooklynCampConstants.PLAN_ID);
            if ((childPlanId != null) && (childPlanId.equals(planId))) {
                return child;
            }
        }
        return null;
    }

    protected void testApplicationSensors(final VanillaCloudFoundryApplication entity) {
        testRunningSensors(entity);
        Asserts.succeedsEventually(
                new Runnable() {
                    public void run() {
                        assertNotNull(entity.getAttribute(Attributes.MAIN_URI).toString());
                        assertNotNull(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL));
                    }
                });
    }

    protected void testRunningSensors(final CloudFoundryEntity entity) {
        Asserts.succeedsEventually(
                new Runnable() {
                    public void run() {
                        assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication
                                .SERVICE_PROCESS_IS_RUNNING));
                    }
                });
    }

    protected CloudFoundryPaasLocation getLocation(CloudFoundryEntity entity) {
        return (CloudFoundryPaasLocation) Iterables.getOnlyElement(entity.getLocations());
    }

    protected String createApplicationUrl(String host) {
        return createApplicationUrl(host, DEFAULT_DOMAIN);
    }

    protected String createApplicationUrl(String host, String domain) {
        return "https://" + host + "." + domain;
    }

}
