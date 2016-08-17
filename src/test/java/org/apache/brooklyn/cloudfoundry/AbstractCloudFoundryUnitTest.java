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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocationTest;
import org.apache.brooklyn.cloudfoundry.location.StubbedCloudFoundryPaasClientRegistry;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.BeforeMethod;

public class AbstractCloudFoundryUnitTest extends BrooklynAppUnitTestSupport
        implements CloudFoundryTestFixtures {

    protected static final String APPLICATION_NAME = UUID.randomUUID().toString().substring(0, 8);

    public static final String BROOKLYN_DOMAIN = "brooklyndomain.io";
    protected static final String MOCK_BUILDPACK = Strings.makeRandomId(20);
    public static final String MOCK_DB_URI_ADDRESS = "mysql://host.net/ad?user=b0e8f";
    public static final String MOCK_JDBC_ADDRESS = "jdbc:" + MOCK_DB_URI_ADDRESS;


    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    public static final String SERVICE_X = "service1";
    public static final String SERVICE_X_PLAN = "planX";
    public static final String SERVICE_INSTANCE_NAME = "serviceInstance";

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation();
    }

    protected CloudFoundryPaasLocation createCloudFoundryPaasLocation() {
        Map<String, String> m = MutableMap.of();
        m.put("user", "super_user");
        m.put("password", "super_secret");
        m.put("org", "secret_organization");
        m.put("endpoint", "https://api.super.secret.io");
        m.put("space", "development");

        LocationSpec<CloudFoundryPaasLocation> spec = LocationSpec
                .create(CloudFoundryPaasLocation.class)
                .configure(m)
                .configure(CloudFoundryPaasLocation.CF_CLIENT_REGISTRY, getStubberRegistry());
        return createCloudFoundryPaasLocation(spec);
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation(
            LocationSpec<CloudFoundryPaasLocation> spec) {
        try {
            return mgmt.getLocationManager().createLocation(spec);
        } catch (NullPointerException e) {
            throw new AssertionError("Failed to create " + CloudFoundryPaasLocation.class.getName() +
                    ". Have you configured brooklyn.location.cloudfoundry.{identity,credential} " +
                    "in your brooklyn.properties file?");
        }
    }

    private StubbedCloudFoundryPaasClientRegistry getStubberRegistry() {
        return new StubbedCloudFoundryPaasClientRegistry();
    }

    public void checkDefaultResourceProfile(VanillaCloudFoundryApplication entity) {
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_MEMORY));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_DISK));
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES),
                entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_INSTANCES));
    }

    public String inferApplicationUrl(ConfigBag params) {
        String host = params.get(VanillaCloudFoundryApplication.APPLICATION_HOST);
        if (Strings.isBlank(host)) {
            host = params.get(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey());
        }
        String domain = params.get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        if (Strings.isBlank(domain)) {
            domain = CloudFoundryPaasLocationTest.BROOKLYN_DOMAIN;
        }
        assertNotNull(host);
        assertNotNull(domain);
        return "https://" + host + "." + domain;
    }

    protected ConfigBag getDefaultServiceConfig() {
        ConfigBag params = ConfigBag.newInstance();
        params.configure(VanillaCloudFoundryService.SERVICE_NAME, SERVICE_X);
        params.configure(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME, SERVICE_INSTANCE_NAME);
        params.configure(VanillaCloudFoundryService.PLAN, SERVICE_X_PLAN);
        return params;
    }

    protected void createServiceAndCheck(Map<String, Object> params) {
        cloudFoundryPaasLocation.createServiceInstance(params);
        assertTrue(cloudFoundryPaasLocation.serviceInstanceExist(SERVICE_INSTANCE_NAME));
    }

    protected void deleteServiceAndCheck(String serviceInstanceName) {
        cloudFoundryPaasLocation.deleteServiceInstance(serviceInstanceName);
        assertFalse(cloudFoundryPaasLocation.serviceInstanceExist(SERVICE_INSTANCE_NAME));
    }

    protected static IllegalArgumentException nonExistentServiceException(String serviceName) {
        return new IllegalArgumentException("Service " + serviceName + " does not exist");
    }
}
