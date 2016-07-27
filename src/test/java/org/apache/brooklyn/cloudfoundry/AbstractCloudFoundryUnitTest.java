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

import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.BeforeMethod;

public class AbstractCloudFoundryUnitTest extends BrooklynAppUnitTestSupport
        implements CloudFoundryTestFixtures {

    protected static final String APPLICATION_NAME = UUID.randomUUID().toString().substring(0, 8);
    protected static final String BROOKLYN_DOMAIN = "brooklyndomain.io";
    protected static final String DEFAULT_APPLICATION_BROOKLYN_DOMAIN
            = APPLICATION_NAME + "." + BROOKLYN_DOMAIN;
    protected static final String DEFAULT_APPLICATION_ADDRESS
            = "https://" + DEFAULT_APPLICATION_BROOKLYN_DOMAIN;
    protected static final String MOCK_BUILDPACK = Strings.makeRandomId(20);
    protected static final String MOCK_HOST = "mockedhost";

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation();
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation() {
        Map<String, String> m = MutableMap.of();
        m.put("user", "super_user");
        m.put("password", "super_secret");
        m.put("org", "secret_organization");
        m.put("endpoint", "https://api.super.secret.io");
        m.put("space", "development");

        return (CloudFoundryPaasLocation)
                mgmt.getLocationRegistry().getLocationManaged("cloudfoundry", m);
    }

    public void checkDefaultResourceProfile(VanillaCloudFoundryApplication entity) {
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_MEMORY));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_DISK));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_INSTANCES));
    }

}
