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

import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.entity.webapp.CloudFoundryWebApp;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.EntityAsserts;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableMap;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class CloudFoundryWebAppLiveTest extends AbstractCloudFoundryPaasLocationLiveTest {

    private final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    private final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    private final static String TEST_ENV_NAME = "test-env-name";
    private final static String TEST_ENV_VALUE = "test-env-value";

    @Test(groups = {"Live"})
    protected void deployApplicationTest() throws Exception {
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
    protected void stopApplicationTest() throws Exception {
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

    @Test(groups = {"Live"})
    protected void settingEnvsTest() throws Exception {
        String envApplicationName = APPLICATION_NAME + "-envs";
        app.createAndManageChild(EntitySpec.create(CloudFoundryWebApp.class)
                .configure(CloudFoundryWebApp.APPLICATION_NAME, envApplicationName)
                .configure(CloudFoundryWebApp.ARTIFACT_URL, APPLICATION_ARTIFACT_URL)
                .configure(CloudFoundryWebApp.ENV, MutableMap.of(TEST_ENV_NAME, TEST_ENV_VALUE)));

        app.start(ImmutableList.of(cloudFoundryPaasLocation));
        EntityAsserts.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);

        Map<String, String> envMap = cloudFoundryPaasLocation.getEnv(envApplicationName);
        assertNotNull(envMap);
        assertTrue(envMap.containsKey(TEST_ENV_NAME));
        assertEquals(envMap.get(TEST_ENV_NAME), TEST_ENV_VALUE);
    }


}
