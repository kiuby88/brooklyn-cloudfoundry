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

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.text.Strings;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

public class VanillaCloudFoundryApplicationTest extends AbstractCloudFoundryUnitTest {

    private static final String APP_PATH = "vanilla-cf-app-test";

    private MockWebServer mockWebServer;
    private HttpUrl serverUrl;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        mockWebServer = new MockWebServer();
        serverUrl = mockWebServer.url(APP_PATH);

        mockWebServer.setDispatcher(getGenericDispatcher());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        super.tearDown();
        mockWebServer.shutdown();
    }

    @Test
    public void testDeployApplication() throws IOException {
        CloudFoundryPaasLocation location = spy(cloudFoundryPaasLocation);
        doNothing().when(location).startApplication(anyString());
        doReturn(serverUrl.url().toString()).when(location).deploy(anyMap());

        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME)
                        .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DOMAIN)
                        .configure(VanillaCloudfoundryApplication.BUILDPACK, Strings.makeRandomId(20)));

        startEntityInLocationAndCheckSensors(entity, location);
    }

    @Test
    public void testStopApplication() {
        CloudFoundryPaasLocation location = spy(cloudFoundryPaasLocation);
        doNothing().when(location).startApplication(anyString());
        doNothing().when(location).stop(anyString());
        doNothing().when(location).delete(anyString());
        doReturn(serverUrl.url().toString()).when(location).deploy(anyMap());

        final VanillaCloudfoundryApplication entity =
                app.createAndManageChild(EntitySpec.create(VanillaCloudfoundryApplication.class)
                        .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME));

        startEntityInLocationAndCheckSensors(entity, location);
        entity.stop();

        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertNull(entity.getAttribute(Startable.SERVICE_UP));
                assertNull(entity.getAttribute(VanillaCloudfoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
    }

    private void startEntityInLocationAndCheckSensors(final VanillaCloudfoundryApplication entity,
                                                      CloudFoundryPaasLocation location) {
        entity.start(ImmutableList.of(location));
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                assertTrue(entity.getAttribute(VanillaCloudfoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
        assertEquals(entity.getAttribute(Attributes.MAIN_URI).toString(), serverUrl.url().toString());
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL), serverUrl.url().toString());
    }

    private Dispatcher getGenericDispatcher() {
        return new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/" + APP_PATH)) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

}
