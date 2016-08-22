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

import java.util.Map;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryYamlLiveTest;
import org.apache.brooklyn.cloudfoundry.entity.service.CloudFoundryService;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.Test;

public class VanillaCloudFoundryApplicationYamlLiveTest extends AbstractCloudFoundryYamlLiveTest {

    @Test(groups = {"Live"})
    public void deploySimpleWebapp() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-standalone.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);
    }

    @Test(groups = {"Live"})
    public void deployWebappWithName() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-app-name.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);
        String name = entity.getAttribute(VanillaCloudFoundryApplication.APPLICATION_NAME);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL),
                createApplicationUrl(name));
    }

    @Test(groups = {"Live"})
    public void deployWebappWitDomain() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-domain.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);

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
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);

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
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);
        Map<String, String> env = entity.getAttribute(VanillaCloudFoundryApplication.ENV);
        assertEquals(env, MutableMap.of("env1", "value1", "env2", "2", "env3", "value3"));
    }

    @Test(groups = {"Live"})
    public void deployWebappResourceProfile() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-resources-profile.yml").getApplication();

        VanillaCloudFoundryApplication entity = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        testUrisApplicationSensors(entity);

        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY).intValue(), 1024);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES).intValue(), 1);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK).intValue(), 2048);
    }

    @Test(groups = {"Live"})
    public void testWebAppWithService() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-with-bound-service.yml").getApplication();

        VanillaCloudFoundryApplication vanillaApp = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        CloudFoundryService vanillaService = (CloudFoundryService)
                findChildEntitySpecByPlanId(app, DEFAULT_SERVICE_ID);

        testUrisApplicationSensors(vanillaApp);
        checkRunningSensors(vanillaService);
        String applicationName =
                vanillaApp.getAttribute(VanillaCloudFoundryApplication.APPLICATION_NAME);
        String serviceInstanceName =
                vanillaService.getAttribute(CloudFoundryService.SERVICE_INSTANCE_NAME);
        assertTrue(getLocation(vanillaService)
                .isServiceBoundTo(serviceInstanceName, applicationName));
    }

}
