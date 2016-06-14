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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.UUID;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.entity.webapp.CloudFoundryWebApp;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.EntityAsserts;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.BrooklynAppLiveTestSupport;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.test.Asserts;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class CloudFoundryWebAppLiveTest extends BrooklynAppLiveTestSupport {

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    protected final String APPLICATION_NAME = "test-brooklyn-application-" + UUID.randomUUID()
            .toString().substring(0, 8);

    private final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    private final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    protected final String LOCATION_SPEC_NAME = "cloudfoundry-instance";

    @BeforeMethod
    public void setUp() throws Exception {
        mgmt = newLocalManagementContext();
        cloudFoundryPaasLocation = newSampleCloudFoundryLocationForTesting(LOCATION_SPEC_NAME);
        app = TestApplication.Factory.newManagedInstanceForTests();
    }

    @Test(groups = {"Live"})
    protected void testDeployApplication() throws Exception {
        final CloudFoundryWebApp server = app.
                createAndManageChild(EntitySpec.create(CloudFoundryWebApp.class)
                        .configure(CloudFoundryWebApp.APPLICATION_NAME, APPLICATION_NAME)
                        .configure(CloudFoundryWebApp.ARTIFACT_URL, APPLICATION_ARTIFACT_URL));

        app.start(ImmutableList.of(cloudFoundryPaasLocation));

        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(server.getAttribute(Startable.SERVICE_UP));
                assertTrue(server.getAttribute(CloudFoundryWebApp
                        .SERVICE_PROCESS_IS_RUNNING));
                assertNotNull(server.getAttribute(Attributes.MAIN_URI));
                assertNotNull(server.getAttribute(CloudFoundryWebApp.ROOT_URL));
            }
        });
    }

    @Test(groups = {"Live"})
    protected void testStopApplication() throws Exception {
        final CloudFoundryWebApp server = app.
                createAndManageChild(EntitySpec.create(CloudFoundryWebApp.class)
                        .configure(CloudFoundryWebApp.APPLICATION_NAME, "stop-" + APPLICATION_NAME)
                        .configure(CloudFoundryWebApp.ARTIFACT_URL, APPLICATION_ARTIFACT_URL));

        app.start(ImmutableList.of(cloudFoundryPaasLocation));
        EntityAsserts.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);

        app.stop();
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertEquals(server.getAttribute(CloudFoundryWebApp
                        .SERVICE_STATE_ACTUAL), Lifecycle.STOPPED);
                assertNull(server.getAttribute(Startable.SERVICE_UP));
                assertNull(server.getAttribute(CloudFoundryWebApp
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
    }

    private CloudFoundryPaasLocation newSampleCloudFoundryLocationForTesting(String spec) {
        return (CloudFoundryPaasLocation) mgmt.getLocationRegistry().getLocationManaged(spec);
    }

    protected LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests(BrooklynProperties.Factory.newDefault());
    }

    public String getClasspathUrlForResource(String resourceName) {
        return "classpath://" + resourceName;
    }

}
