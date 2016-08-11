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
package org.apache.brooklyn.cloudfoundry.entity.service;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.v2.CloudFoundryException;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class VanillaCloudFoundryServiceTest extends AbstractCloudFoundryUnitTest {


    private CloudFoundryPaasLocation location;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        location = spy(cloudFoundryPaasLocation);
    }

    @Test
    public void testCreateService() throws IOException {
        doNothing().when(location).createServiceInstance(anyMap());
        doReturn(true).when(location).serviceInstanceExist(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp();
        startServiceInLocationAndCheckSensors(entity, location);
        assertTrue(Strings
                .isNonBlank(entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID)));
    }

    @Test
    public void testCreateServiceWithName() throws IOException {
        doNothing().when(location).createServiceInstance(anyMap());
        doReturn(true).when(location).serviceInstanceExist(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp(SERVICE_INSTANCE_NAME);
        startServiceInLocationAndCheckSensors(entity, location);

        assertEquals(entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID),
                SERVICE_INSTANCE_NAME);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateRepeatedService() throws IOException {
        doThrow(repeatedServiceException(SERVICE_INSTANCE_NAME))
                .when(location).createServiceInstance(anyMap());

        VanillaCloudFoundryService entity = addDefaultServiceToApp(SERVICE_INSTANCE_NAME);
        startServiceInLocationAndCheckSensors(entity, location);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInvalidService() throws IOException {
        doThrow(invalidServiceException(SERVICE_INSTANCE_NAME))
                .when(location).createServiceInstance(anyMap());
        startServiceInLocationAndCheckSensors(addDefaultServiceToApp(), location);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInvalidPlan() throws IOException {
        doThrow(invalidPlanException(SERVICE_X_PLAN))
                .when(location).createServiceInstance(anyMap());
        startServiceInLocationAndCheckSensors(addDefaultServiceToApp(), location);
    }

    @Test
    public void testStopService() throws IOException {
        doNothing().when(location).createServiceInstance(anyMap());
        doReturn(true).when(location).serviceInstanceExist(anyString());
        doNothing().when(location).deleteServiceInstance(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp();
        startServiceInLocationAndCheckSensors(entity, location);

        String serviceInstanceId =
                entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_ID);
        stopServiceAndCheckSensors(entity);
        verify(location, times(1)).deleteServiceInstance(serviceInstanceId);
    }

    private void stopServiceAndCheckSensors(VanillaCloudFoundryService service) {
        service.stop();
        assertNull(service.getAttribute(VanillaCloudFoundryService.SERVICE_UP));
        assertNull(service.getAttribute(VanillaCloudFoundryService.SERVICE_PROCESS_IS_RUNNING));
    }

    private void startServiceInLocationAndCheckSensors(VanillaCloudFoundryService entity,
                                                       CloudFoundryPaasLocation location) {
        entity.start(ImmutableList.of(location));
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertTrue(entity.getAttribute(VanillaCloudFoundryService.SERVICE_UP));
                assertTrue(entity.getAttribute(VanillaCloudFoundryService.SERVICE_PROCESS_IS_RUNNING));
                assertEquals(entity.getAttribute(VanillaCloudFoundryService.SERVICE_STATE_ACTUAL),
                        Lifecycle.RUNNING);
            }
        });
    }

    private VanillaCloudFoundryService addDefaultServiceToApp() {
        return addDefaultServiceToApp(Strings.EMPTY);
    }

    private VanillaCloudFoundryService addDefaultServiceToApp(String serviceInstanceName) {
        EntitySpec<VanillaCloudFoundryService> vanilla = EntitySpec
                .create(VanillaCloudFoundryService.class)
                .configure(VanillaCloudFoundryService.SERVICE_NAME, SERVICE_X)
                .configure(VanillaCloudFoundryService.PLAN, SERVICE_X_PLAN);
        if (Strings.isNonBlank(serviceInstanceName)) {
            vanilla.configure(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME,
                    serviceInstanceName);
        }
        return app.createAndManageChild(vanilla);
    }

    private static CloudFoundryException repeatedServiceException(String instanceName) {
        return new CloudFoundryException(60002, "The service instance name is taken: "
                + instanceName, "CF-ServiceInstanceNameTaken");
    }

    private static IllegalArgumentException invalidServiceException(String service) {
        return new IllegalArgumentException("Service " + service + " does not exist");
    }

    private static IllegalArgumentException invalidPlanException(String plan) {
        return new IllegalArgumentException("Service plan " + plan + " does not exist");
    }

}
