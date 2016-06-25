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
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.util.collections.MutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationTest extends BrooklynAppUnitTestSupport {

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation();
    }

    @Test
    public void testDeploy() {
        CloudFoundryPaasApplication deployedApplication = cloudFoundryPaasLocation.deploy();

        assertEquals(cloudFoundryPaasLocation.getDeployedApplications().size(), 1);
        assertTrue(cloudFoundryPaasLocation.getDeployedApplications().contains(deployedApplication));
    }

    @Test
    public void testUndeploy() {
        CloudFoundryPaasApplication deployedApplication = cloudFoundryPaasLocation.deploy();
        assertEquals(cloudFoundryPaasLocation.getDeployedApplications().size(), 1);

        cloudFoundryPaasLocation.undeploy(deployedApplication);
        assertTrue(cloudFoundryPaasLocation.getDeployedApplications().isEmpty());
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation() {
        Map<String, String> m = MutableMap.of();
        m.put("identity", "super_user");
        m.put("credential", "super_secret");
        m.put("org", "secret_organization");
        m.put("endpoint", "https://api.super.secret.io");
        m.put("space", "development");

        return (CloudFoundryPaasLocation)
                mgmt.getLocationRegistry().getLocationManaged("cloudfoundry", m);
    }
}
