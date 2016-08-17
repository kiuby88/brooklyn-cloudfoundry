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

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.v2.CloudFoundryException;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VanillaCloudFoundryServiceTest extends AbstractCloudFoundryUnitTest {

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        cloudFoundryPaasLocation = spy(createCloudFoundryPaasLocation());
    }

    @Test
    public void testCreateService() throws IOException {
        doNothing().when(cloudFoundryPaasLocation).createServiceInstance(anyMap());
        doReturn(true).when(cloudFoundryPaasLocation).serviceInstanceExist(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp();
        startServiceInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
        assertTrue(Strings
                .isNonBlank(entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME)));
    }

    @Test
    public void testCreateServiceWithName() throws IOException {
        doNothing().when(cloudFoundryPaasLocation).createServiceInstance(anyMap());
        doReturn(true).when(cloudFoundryPaasLocation).serviceInstanceExist(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp(SERVICE_INSTANCE_NAME);
        startServiceInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);

        assertEquals(entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME),
                SERVICE_INSTANCE_NAME);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateRepeatedService() throws IOException {
        doThrow(repeatedServiceException(SERVICE_INSTANCE_NAME))
                .when(cloudFoundryPaasLocation).createServiceInstance(anyMap());

        VanillaCloudFoundryService entity = addDefaultServiceToApp(SERVICE_INSTANCE_NAME);
        startServiceInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInvalidService() throws IOException {
        doThrow(invalidServiceException(SERVICE_INSTANCE_NAME))
                .when(cloudFoundryPaasLocation).createServiceInstance(anyMap());
        startServiceInLocationAndCheckSensors(addDefaultServiceToApp(), cloudFoundryPaasLocation);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testCreateInvalidPlan() throws IOException {
        doThrow(invalidPlanException(SERVICE_X_PLAN))
                .when(cloudFoundryPaasLocation).createServiceInstance(anyMap());
        startServiceInLocationAndCheckSensors(addDefaultServiceToApp(), cloudFoundryPaasLocation);
    }

    @Test
    public void testStopService() throws IOException {
        doNothing().when(cloudFoundryPaasLocation).createServiceInstance(anyMap());
        doReturn(MutableList.of()).when(cloudFoundryPaasLocation).getBoundApplications(anyString());
        doReturn(true).when(cloudFoundryPaasLocation).serviceInstanceExist(anyString());
        doNothing().when(cloudFoundryPaasLocation).deleteServiceInstance(anyString());

        VanillaCloudFoundryService entity = addDefaultServiceToApp();
        startServiceInLocationAndCheckSensors(entity, cloudFoundryPaasLocation);

        String serviceInstanceId =
                entity.getAttribute(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME);
        stopServiceAndCheckSensors(entity);
        verify(cloudFoundryPaasLocation, times(1)).deleteServiceInstance(serviceInstanceId);
    }

    private void stopServiceAndCheckSensors(VanillaCloudFoundryService service) {
        service.stop();
        assertNull(service.getAttribute(VanillaCloudFoundryService.SERVICE_UP));
        assertNull(service.getAttribute(VanillaCloudFoundryService.SERVICE_PROCESS_IS_RUNNING));
    }

    protected VanillaCloudFoundryService addDefaultServiceToApp() {
        return addDefaultServiceToApp(Strings.EMPTY);
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
