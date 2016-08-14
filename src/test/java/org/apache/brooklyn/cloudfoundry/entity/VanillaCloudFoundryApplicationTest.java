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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.service.ServiceOperation;
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;
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
    private String serverAddress;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        cloudFoundryPaasLocation = spy(createCloudFoundryPaasLocation());

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
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());

        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());
        doNothing().when(cloudFoundryPaasLocation).setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication.ENV).isEmpty());
        verify(cloudFoundryPaasLocation, never()).setEnv(APPLICATION_NAME, EMPTY_ENV);
        checkDefaultResourceProfile(entity);
    }

    @Test
    public void testDeployApplicationWithEnv() throws IOException {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(SIMPLE_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());
        doNothing().when(cloudFoundryPaasLocation)
                .setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudFoundryApplication entity = addDefaultVanillaToAppAndMockProfileMethods(
                cloudFoundryPaasLocation, MutableMap.copyOf(SIMPLE_ENV));
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        assertEquals(cloudFoundryPaasLocation.getEnv(APPLICATION_NAME), SIMPLE_ENV);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), SIMPLE_ENV);
        verify(cloudFoundryPaasLocation, times(1)).setEnv(APPLICATION_NAME, SIMPLE_ENV);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployApplicationWithoutLocation() throws IOException {
        final VanillaCloudFoundryApplication entity = addDefaultVanillaEntityChildToApp();
        entity.start(ImmutableList.<Location>of());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetEnvEffector() throws IOException {
        Map<String, String> env = MutableMap.copyOf(EMPTY_ENV);
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(env).when(cloudFoundryPaasLocation).getEnv(anyString());
        doNothing().when(cloudFoundryPaasLocation)
                .setEnv(anyString(), anyMapOf(String.class, String.class));

        VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        assertTrue(entity.getAttribute(VanillaCloudFoundryApplication.ENV).isEmpty());
        entity.setEnv("k1", "v1");
        env.put("k1", "v1");
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), env);
        entity.setEnv("k2", "v2");
        env.put("k2", "v2");
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ENV), env);
    }

    @Test
    public void testSetMemory() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());

        VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        checkDefaultResourceProfile(entity);
        doReturn(CUSTOM_MEMORY).when(cloudFoundryPaasLocation).getMemory(anyString());

        entity.setMemory(CUSTOM_MEMORY);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_MEMORY).intValue(), CUSTOM_MEMORY);
        verify(cloudFoundryPaasLocation, times(1)).setMemory(APPLICATION_NAME, CUSTOM_MEMORY);
    }

    @Test
    public void testSetDisk() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());

        VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        checkDefaultResourceProfile(entity);
        doReturn(CUSTOM_DISK).when(cloudFoundryPaasLocation).getDiskQuota(anyString());

        entity.setDiskQuota(CUSTOM_DISK);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ALLOCATED_DISK).intValue(),
                CUSTOM_DISK);
        verify(cloudFoundryPaasLocation, times(1)).setDiskQuota(APPLICATION_NAME, CUSTOM_DISK);
    }

    @Test
    public void testSetInstances() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());

        doNothing().when(cloudFoundryPaasLocation).setInstancesNumber(anyString(), anyInt());

        VanillaCloudFoundryApplication entity = addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        checkDefaultResourceProfile(entity);
        doReturn(CUSTOM_INSTANCES).when(cloudFoundryPaasLocation).getInstancesNumber(anyString());

        entity.setInstancesNumber(CUSTOM_INSTANCES);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.INSTANCES).intValue(),
                CUSTOM_INSTANCES);
        verify(cloudFoundryPaasLocation, times(1)).setInstancesNumber(APPLICATION_NAME, CUSTOM_INSTANCES);
    }

    @Test
    public void testStopApplication() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doNothing().when(cloudFoundryPaasLocation).stopApplication(anyString());
        doNothing().when(cloudFoundryPaasLocation).deleteApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());

        final VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);

        entity.stop();
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertNull(entity.getAttribute(Startable.SERVICE_UP));
                assertNull(entity.getAttribute(VanillaCloudFoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
    }

    @Test
    public void testRestartApplication() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doNothing().when(cloudFoundryPaasLocation).restartApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());

        final VanillaCloudFoundryApplication entity =
                addDefaultVanillaToAppAndMockProfileMethods(cloudFoundryPaasLocation);
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);

        entity.restart();
        verify(cloudFoundryPaasLocation, times(1)).restartApplication(APPLICATION_NAME);
    }

    @Test
    public void testBindServiceToEntity() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());
        doNothing().when(cloudFoundryPaasLocation)
                .bindServiceToApplication(anyString(), anyString());

        VanillaCloudFoundryApplication entity = addDefaultVanillaToAppAndMockProfileMethods(
                cloudFoundryPaasLocation, MutableList.of(SERVICE_INSTANCE_NAME));

        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        verify(cloudFoundryPaasLocation, times(1))
                .bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testBindNonExistentServiceToEntity() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doThrow(nonExistentServiceException(SERVICE_INSTANCE_NAME))
                .when(cloudFoundryPaasLocation)
                .bindServiceToApplication(anyString(), anyString());

        VanillaCloudFoundryApplication entity = addDefaultVanillaToAppAndMockProfileMethods(
                cloudFoundryPaasLocation, MutableList.of(SERVICE_INSTANCE_NAME));
        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
    }

    @Test
    public void testBindServiceWithOperationToEntity() {
        doNothing().when(cloudFoundryPaasLocation).startApplication(anyString());
        doReturn(serverAddress).when(cloudFoundryPaasLocation).deploy(anyMap());
        doReturn(EMPTY_ENV).when(cloudFoundryPaasLocation).getEnv(anyString());
        doNothing().when(cloudFoundryPaasLocation)
                .bindServiceToApplication(anyString(), anyString());
        ServiceOperation serviceEntity = mock(ServiceOperation.class);
        doNothing().when(serviceEntity).operationAfterBindingTo(anyString());
        doReturn(true).when(serviceEntity).getAttribute(Startable.SERVICE_UP);
        doReturn(SERVICE_INSTANCE_NAME)
                .when(serviceEntity)
                .getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID);

        VanillaCloudFoundryApplication entity = addDefaultVanillaToAppAndMockProfileMethods(
                cloudFoundryPaasLocation, MutableList.of(serviceEntity));

        startEntityInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        verify(cloudFoundryPaasLocation, times(1))
                .bindServiceToApplication(SERVICE_INSTANCE_NAME, APPLICATION_NAME);
    }

    private VanillaCloudFoundryApplication addDefaultVanillaToAppAndMockProfileMethods(
            CloudFoundryPaasLocation location) {
        return addDefaultVanillaToAppAndMockProfileMethods(location, null, null);
    }

    private VanillaCloudFoundryApplication addDefaultVanillaToAppAndMockProfileMethods(
            CloudFoundryPaasLocation location, Map<String, String> env) {
        return addDefaultVanillaToAppAndMockProfileMethods(location, env, null);
    }

    private VanillaCloudFoundryApplication addDefaultVanillaToAppAndMockProfileMethods(
            CloudFoundryPaasLocation location, List<Object> services) {
        return addDefaultVanillaToAppAndMockProfileMethods(location, null, services);
    }

    private VanillaCloudFoundryApplication addDefaultVanillaToAppAndMockProfileMethods(
            CloudFoundryPaasLocation location, Map<String, String> env, List<Object> services) {
        VanillaCloudFoundryApplication entity = addDefaultVanillaEntityChildToApp(env, services);
        mockLocationProfileUsingEntityConfig(location, entity);
        return entity;
    }

    private VanillaCloudFoundryApplication addDefaultVanillaEntityChildToApp() {
        return addDefaultVanillaEntityChildToApp(null, null);
    }

    private VanillaCloudFoundryApplication addDefaultVanillaEntityChildToApp(
            Map<String, String> env, List<Object> services) {
        EntitySpec<VanillaCloudFoundryApplication> vanilla = EntitySpec
                .create(VanillaCloudFoundryApplication.class)
                .configure(VanillaCloudFoundryApplication.APPLICATION_NAME, APPLICATION_NAME)
                .configure(VanillaCloudFoundryApplication.ARTIFACT_PATH, ARTIFACT_URL)
                .configure(VanillaCloudFoundryApplication.APPLICATION_DOMAIN, BROOKLYN_DOMAIN)
                .configure(VanillaCloudFoundryApplication.BUILDPACK, MOCK_BUILDPACK);
        if (env != null) {
            vanilla.configure(VanillaCloudFoundryApplication.ENV, env);
        }
        if (services != null) {
            vanilla.configure(VanillaCloudFoundryApplication.SERVICES, services);
        }
        return app.createAndManageChild(vanilla);
    }

    private void mockLocationProfileUsingEntityConfig(CloudFoundryPaasLocation location,
                                                      VanillaCloudFoundryApplication entity) {
        if (new MockUtil().isMock(location)) {
            doNothing().when(location).setMemory(anyString(), anyInt());
            doNothing().when(location).setDiskQuota(anyString(), anyInt());
            doNothing().when(location).setInstancesNumber(anyString(), anyInt());

            doReturn(entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_MEMORY))
                    .when(location).getMemory(anyString());
            doReturn(entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_DISK))
                    .when(location).getDiskQuota(anyString());
            doReturn(entity.getConfig(VanillaCloudFoundryApplication.REQUIRED_INSTANCES))
                    .when(location).getInstancesNumber(anyString());
        }
    }

    private void startEntityInLocationAndCheckSensors(final VanillaCloudFoundryApplication entity,
                                                      CloudFoundryPaasLocation location) {
        entity.start(ImmutableList.of(location));
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(entity.getAttribute(Startable.SERVICE_UP));
                assertTrue(entity.getAttribute(VanillaCloudFoundryApplication
                        .SERVICE_PROCESS_IS_RUNNING));
            }
        });
        assertEquals(entity.getAttribute(Attributes.MAIN_URI).toString(), serverAddress);
        assertEquals(entity.getAttribute(VanillaCloudFoundryApplication.ROOT_URL), serverAddress);
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
