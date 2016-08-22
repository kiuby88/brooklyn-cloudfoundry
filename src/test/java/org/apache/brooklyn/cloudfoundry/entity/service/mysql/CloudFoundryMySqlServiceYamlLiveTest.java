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
package org.apache.brooklyn.cloudfoundry.entity.service.mysql;

import static org.testng.Assert.assertTrue;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryYamlLiveTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.entity.service.CloudFoundryService;
import org.testng.annotations.Test;

public class CloudFoundryMySqlServiceYamlLiveTest extends AbstractCloudFoundryYamlLiveTest {

    @Test(groups = {"Live"})
    public void testMysqlServiceCreationAndBinding() {
        launcher.setShutdownAppsOnExit(true);
        Application app = launcher.launchAppYaml("vanilla-cf-with-mysql-service.yml").getApplication();

        VanillaCloudFoundryApplication vanillaApp = (VanillaCloudFoundryApplication)
                findChildEntitySpecByPlanId(app, DEFAULT_APP_ID);
        CloudFoundryMySqlService service = (CloudFoundryMySqlService)
                findChildEntitySpecByPlanId(app, DEFAULT_SERVICE_ID);

        testUrisApplicationSensors(vanillaApp);
        checkRunningSensors(service);
        String applicationName =
                vanillaApp.getAttribute(VanillaCloudFoundryApplication.APPLICATION_NAME);
        String serviceInstanceName =
                service.getAttribute(CloudFoundryService.SERVICE_INSTANCE_NAME);
        assertTrue(getLocation(service)
                .isServiceBoundTo(serviceInstanceName, applicationName));
    }

}
