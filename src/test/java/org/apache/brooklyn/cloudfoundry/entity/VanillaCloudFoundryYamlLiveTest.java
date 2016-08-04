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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.camp.brooklyn.BrooklynCampConstants;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.launcher.SimpleYamlLauncherForTests;
import org.apache.brooklyn.launcher.camp.SimpleYamlLauncher;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VanillaCloudFoundryYamlLiveTest {

    private static final String DEFAULT_ID = "vanilla-app";
    private static final String DEFAULT_DOMAIN = "cfapps.io";

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
    public void deploySimpleWebapp() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-standalone.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);
    }

    @Test(groups = {"Live"})
    public void deployWebappWithName() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-app-name.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);
        String name = entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_NAME);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL),
                createApplicationUrl(name));
    }

    @Test(groups = {"Live"})
    public void deployWebappWitDomain() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-domain.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);

        String domain = entity.getConfig(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        assertFalse(Strings.isBlank(domain));
        String name = entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_NAME);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL),
                createApplicationUrl(name, domain));
    }

    @Test(groups = {"Live"})
    public void deployWebappWitHostAndDomain() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-host-and-domain.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);

        String domain = entity.getConfig(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        String host = entity.getConfig(VanillaCloudFoundryApplication.APPLICATION_HOST);
        assertFalse(Strings.isBlank(domain));
        assertFalse(Strings.isBlank(host));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL),
                createApplicationUrl(host, domain));
    }

    @Test(groups = {"Live"})
    @SuppressWarnings("unchecked")
    public void deployWebappWithEnv() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-env.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);
        Map<String, String> env = entity.getAttribute(VanillaCloudFoundryApplication.ENV);
        assertEquals(env, MutableMap.of("env1", "value1", "env2", "2", "env3", "value3"));
    }

    @Test(groups = {"Live"})
    public void deployWebappResourceProfile() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-resources-profile.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_ID);
        testEntitySensors(entity);

        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY).intValue(), 1024);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES).intValue(), 1);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK).intValue(), 2048);
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

    private void testEntitySensors(final VanillaCloudFoundryApplication entity) {
        Asserts.succeedsEventually(
                new Runnable() {
                    public void run() {
                        assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication
                                .SERVICE_PROCESS_IS_RUNNING));
                        assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                        assertNotNull(entity.getAttribute(Attributes.MAIN_URI).toString());
                        assertNotNull(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL));
                    }
                });
    }

    private String createApplicationUrl(String host) {
        return createApplicationUrl(host, DEFAULT_DOMAIN);
    }

    private String createApplicationUrl(String host, String domain) {
        return "https://" + host + "." + domain;
    }

}
