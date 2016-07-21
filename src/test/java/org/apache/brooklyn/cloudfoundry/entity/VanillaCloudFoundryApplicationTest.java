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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
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

    private static final String MOCKED_APP_PATH = "vanilla-cf-app-test";

    private MockWebServer mockWebServer;
    private HttpUrl serverUrl;
    private CloudFoundryPaasLocation location;
    private String serverAddress;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        location = spy(cloudFoundryPaasLocation);

        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(getGenericDispatcher());
        serverUrl = mockWebServer.url(MOCKED_APP_PATH);
        serverAddress = serverUrl.url().toString();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        super.tearDown();
        mockWebServer.shutdown();
    }

    @Test
    public void testDeployApplication() throws IOException {
        doNothing().when(location).startApplication(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(EMPTY_ENV).when(location).getEnv(anyString());
        doNothing().when(location).setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
        startEntityInLocationAndCheckSensors(entity, location);
        assertTrue(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV).isEmpty());
        verify(location, never()).setEnv(APPLICATION_NAME, EMPTY_ENV);
        checkDefaultResourceProfile(entity);
    }

    @Test
    public void testDeployApplicationWithEnv() throws IOException {
        MutableMap<String, String> env = MutableMap.copyOf(SIMPLE_ENV);
        doNothing().when(location).startApplication(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(SIMPLE_ENV).when(location).getEnv(anyString());
        doNothing().when(location).setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp(env);
        startEntityInLocationAndCheckSensors(entity, location);
        assertEquals(location.getEnv(APPLICATION_NAME), SIMPLE_ENV);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV), SIMPLE_ENV);
        verify(location, times(1)).setEnv(APPLICATION_NAME, SIMPLE_ENV);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployApplicationWithoutLocation() throws IOException {
        final VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
        entity.start(ImmutableList.<Location>of());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetEnvEffector() throws IOException {
        Map<String, String> env = MutableMap.copyOf(EMPTY_ENV);
        CloudFoundryPaasLocation location = spy(cloudFoundryPaasLocation);
        doNothing().when(location).startApplication(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(env).when(location).getEnv(anyString());
        doNothing().when(location).setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
        startEntityInLocationAndCheckSensors(entity, location);
        assertTrue(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV).isEmpty());
        entity.setEnv("k1", "v1");
        env.put("k1", "v1");
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV), env);
        entity.setEnv("k2", "v2");
        env.put("k2", "v2");
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.APPLICATION_ENV), env);
    }

    @Test
    public void testModifyResourcesProfile() {
        doNothing().when(location).startApplication(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(EMPTY_ENV).when(location).getEnv(anyString());
        doNothing().when(location).setMemory(anyString(), anyInt());
        doReturn(CUSTOM_MEMORY).when(location).getMemory(anyString());
        doNothing().when(location).setDiskQuota(anyString(), anyInt());
        doReturn(CUSTOM_DISK).when(location).getDiskQuota(anyString());
        doNothing().when(location).setInstancesNumber(anyString(), anyInt());
        doReturn(CUSTOM_INSTANCES).when(location).getInstancesNumber(anyString());

        VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
        startEntityInLocationAndCheckSensors(entity, location);
        checkDefaultResourceProfile(entity);

        entity.setMemory(CUSTOM_MEMORY);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_MEMORY).intValue(), CUSTOM_MEMORY);
        verify(location, times(1)).setMemory(APPLICATION_NAME, CUSTOM_MEMORY);

        entity.setDiskQuota(CUSTOM_DISK);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ALLOCATED_DISK).intValue(), CUSTOM_DISK);
        verify(location, times(1)).setDiskQuota(APPLICATION_NAME, CUSTOM_DISK);

        entity.setInstancesNumber(CUSTOM_INSTANCES);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.INSTANCES).intValue(), CUSTOM_INSTANCES);
        verify(location, times(1)).setInstancesNumber(APPLICATION_NAME, CUSTOM_INSTANCES);
    }

    @Test
    public void testStopApplication() {
        doNothing().when(location).startApplication(anyString());
        doNothing().when(location).stop(anyString());
        doNothing().when(location).delete(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(EMPTY_ENV).when(location).getEnv(anyString());

        final VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
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

    @Test
    public void testRestartApplication() {
        doNothing().when(location).startApplication(anyString());
        doNothing().when(location).restart(anyString());
        doReturn(serverAddress).when(location).deploy(anyMap());
        doReturn(EMPTY_ENV).when(location).getEnv(anyString());

        final VanillaCloudfoundryApplication entity = addDefaultVanillaEntityChildToApp();
        startEntityInLocationAndCheckSensors(entity, location);

        entity.restart();
        verify(location, times(1)).restart(APPLICATION_NAME);
    }

    private VanillaCloudfoundryApplication addDefaultVanillaEntityChildToApp() {
        return addDefaultVanillaEntityChildToApp(null);
    }

    private VanillaCloudfoundryApplication addDefaultVanillaEntityChildToApp(Map<String, String> env) {
        EntitySpec<VanillaCloudfoundryApplication> vanilla = EntitySpec.create(VanillaCloudfoundryApplication.class)
                .configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME)
                .configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL)
                .configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN)
                .configure(VanillaCloudfoundryApplication.BUILDPACK, MOCK_BUILDPACK);
        if (env != null) {
            vanilla.configure(VanillaCloudfoundryApplication.ENV, env);
        }
        return app.createAndManageChild(vanilla);
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
        assertEquals(entity.getAttribute(Attributes.MAIN_URI).toString(), serverAddress);
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL), serverAddress);
    }

    private Dispatcher getGenericDispatcher() {
        return new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/" + MOCKED_APP_PATH)) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

}
