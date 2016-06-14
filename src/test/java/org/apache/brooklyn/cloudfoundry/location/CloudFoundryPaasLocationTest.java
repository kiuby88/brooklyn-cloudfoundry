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
package org.apache.brooklyn.cloudfoundry.location;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.Staging;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationTest extends AbstractCloudFoundryUnitTest {

    protected static final String APPLICATION_NAME = UUID.randomUUID().toString().substring(0, 8);
    protected static final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    protected final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    private static final String DEFAULT_DOMAIN = "brooklyndomain.io";
    private static final String DEFAULT_APPLICATION_DOMAIN
            = APPLICATION_NAME + "." + DEFAULT_DOMAIN;
    private static final String DEFAULT_APPLICATION_ADDRESS
            = "https://" + APPLICATION_NAME + "." + DEFAULT_DOMAIN;

    @Mock
    protected CloudFoundryClient client;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testSetUpClient() {
        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.setUpClient();
        assertNotNull(cloudFoundryPaasLocation.getClient());
    }

    @Test
    public void testClientSingletonManagement() {
        cloudFoundryPaasLocation.setClient(client);

        CloudFoundryClient client1 = cloudFoundryPaasLocation.getClient();
        cloudFoundryPaasLocation.setUpClient();
        CloudFoundryClient client2 = cloudFoundryPaasLocation.getClient();
        assertEquals(client1, client2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeployApplication() throws IOException {
        doNothing().when(client).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyList(),
                        Matchers.anyList());
        doNothing().when(client).uploadApplication(Matchers.anyString(), anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getUris()).thenReturn(MutableList.of(DEFAULT_APPLICATION_DOMAIN));
        when(client.getApplication(anyString())).thenReturn(cloudApp);

        CloudDomain cloudDomain = mock(CloudDomain.class);
        when(cloudDomain.getName()).thenReturn(DEFAULT_DOMAIN);
        when(client.getDefaultDomain()).thenReturn(cloudDomain);

        cloudFoundryPaasLocation.setClient(client);
        String applicationDomain = cloudFoundryPaasLocation
                .deploy(APPLICATION_NAME, Strings.makeRandomId(15), APPLICATION_ARTIFACT_URL);

        assertEquals(applicationDomain, DEFAULT_APPLICATION_ADDRESS);
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployNonExistentArtifact() throws IOException {
        doNothing().when(client).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyList(),
                        Matchers.anyList());
        doThrow(new PropagatedRuntimeException(new FileNotFoundException()))
                .when(client).uploadApplication(Matchers.anyString(), anyString());

        CloudDomain cloudDomain = mock(CloudDomain.class);
        when(cloudDomain.getName()).thenReturn(DEFAULT_DOMAIN);
        when(client.getDefaultDomain()).thenReturn(cloudDomain);

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation
                .deploy(APPLICATION_NAME, Strings.makeRandomId(15), Strings.makeRandomId(15));
    }

    @Test
    public void testStartApplication() {
        when(client.startApplication(anyString())).thenReturn(new StartingInfo(Strings.EMPTY));

        cloudFoundryPaasLocation.setClient(client);
        StartingInfo startingInfo = cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
        assertNotNull(startingInfo);
        assertEquals(startingInfo.getStagingFile(), Strings.EMPTY);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStartNonExistentApplication() {
        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(client).startApplication(Matchers.anyString());

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.startApplication(APPLICATION_NAME);
    }

    @Test
    public void testStopApplication() {
        doNothing().when(client).stopApplication(Matchers.anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        when(client.getApplication(anyString())).thenReturn(cloudApp);

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
        CloudApplication.AppState state = cloudFoundryPaasLocation
                .getApplicationStatus(APPLICATION_NAME);
        assertEquals(state, CloudApplication.AppState.STOPPED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStopNonExistentApplication() {
        when(client.getApplication(anyString())).thenReturn(null);

        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(client).stopApplication(Matchers.anyString());

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.stopApplication(APPLICATION_NAME);
    }

    @Test
    public void testDeleteApplication() {
        doNothing().when(client).deleteApplication(Matchers.anyString());
        when(client.getApplication(anyString())).thenReturn(null);

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.deleteApplication(APPLICATION_NAME);
        assertFalse(cloudFoundryPaasLocation.isDeployed(APPLICATION_NAME));
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testDeleteNonExistentApplication() {
        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(client).stopApplication(Matchers.anyString());

        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testGetApplicationStatus() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getState()).thenReturn(CloudApplication.AppState.STARTED);
        when(client.getApplication(anyString())).thenReturn(cloudApp);

        cloudFoundryPaasLocation.setClient(client);
        CloudApplication.AppState state = cloudFoundryPaasLocation
                .getApplicationStatus(APPLICATION_NAME);
        assertEquals(state, CloudApplication.AppState.STARTED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testNonExistentApplicationStatus() {
        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(client).getApplication(Matchers.anyString());
        cloudFoundryPaasLocation.setClient(client);
        cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testIfAnApplicationIsDeployed() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(client.getApplication(anyString())).thenReturn(cloudApp);

        cloudFoundryPaasLocation.setClient(client);
        assertTrue(cloudFoundryPaasLocation.isDeployed(Strings.makeRandomId(10)));
    }

    @Test
    public void testIfAnApplicationIsNotDeployed() {
        when(client.getApplication(anyString())).thenReturn(null);

        cloudFoundryPaasLocation.setClient(client);
        assertFalse(cloudFoundryPaasLocation.isDeployed(Strings.makeRandomId(10)));
    }

}
