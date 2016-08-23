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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudFoundryApplication;
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.BrooklynAppLiveTestSupport;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.text.Strings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class AbstractCloudFoundryLiveTest extends BrooklynAppLiveTestSupport
        implements CloudFoundryTestFixtures {

    protected static final String APPLICATION_NAME_PREFIX = "test-brooklyn-app";
    protected static final String DEFAULT_DOMAIN = "cfapps.io";
    protected final String LOCATION_SPEC_NAME = "pivotal-ws";
    protected final String JAVA_BUILDPACK = "https://github.com/cloudfoundry/java-buildpack.git";
    protected final String CLEARDB_SERVICE = "cleardb";
    protected final String CLEARDB_SPARK_PLAN = "spark";
    protected final String SERVICE_INSTANCE_NAME = "test-service";


    protected String applicationName;
    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        mgmt = newLocalManagementContext();
        cloudFoundryPaasLocation = newSampleCloudFoundryLocationForTesting(LOCATION_SPEC_NAME);
        applicationName = APPLICATION_NAME_PREFIX + UUID.randomUUID()
                .toString().substring(0, 8);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        super.tearDown();
        if (app != null) {
            Entities.destroyAll(app.getManagementContext());
        }
    }

    protected CloudFoundryPaasLocation newSampleCloudFoundryLocationForTesting(String spec) {
        return (CloudFoundryPaasLocation) mgmt.getLocationRegistry().getLocationManaged(spec);
    }

    protected LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests(BrooklynProperties.Factory.newDefault());
    }

    public static String inferApplicationUrl(Map<String, Object> params) {
        String host = (String) params
                .get(VanillaCloudFoundryApplication.APPLICATION_HOST.getName());
        if (Strings.isBlank(host)) {
            host = (String) params
                    .get(VanillaCloudFoundryApplication.APPLICATION_NAME.getConfigKey().getName());
        }
        String domain = (String) params
                .get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN.getName());
        if (Strings.isBlank(domain)) {
            domain = DEFAULT_DOMAIN;
        }
        assertNotNull(host);
        assertNotNull(domain);
        return "https://" + host + "." + domain;
    }

    protected ConfigBag getDefaultClearDbServiceConfig() {
        ConfigBag params = ConfigBag.newInstance();
        params.configure(VanillaCloudFoundryService.SERVICE_NAME, CLEARDB_SERVICE);
        params.configure(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME, SERVICE_INSTANCE_NAME);
        params.configure(VanillaCloudFoundryService.PLAN, CLEARDB_SPARK_PLAN);
        return params;
    }

    protected void createServiceAndCheck(Map<String, Object> params) {
        cloudFoundryPaasLocation.createServiceInstance(params);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(cloudFoundryPaasLocation.serviceInstanceExist(SERVICE_INSTANCE_NAME));
            }
        });
    }

    protected void deleteServiceAndCheck(String serviceName) {
        cloudFoundryPaasLocation.deleteServiceInstance(serviceName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertFalse(cloudFoundryPaasLocation.serviceInstanceExist(serviceName));
            }
        });
    }

}
