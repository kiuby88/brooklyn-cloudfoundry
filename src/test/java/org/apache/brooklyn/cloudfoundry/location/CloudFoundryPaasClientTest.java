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

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudfoundryApplication;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.config.ConfigBag;
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
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasClientTest extends AbstractCloudFoundryUnitTest {

    @Mock
    private CloudFoundryClient cloudFoundryClient;

    private CloudFoundryPaasClient client;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        client = spy(new CloudFoundryPaasClient(cloudFoundryPaasLocation));
        doReturn(cloudFoundryClient).when(client).getClient();
    }

    @Test
    public void testDeployApplication() throws IOException {
        doNothing().when(cloudFoundryClient).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyInt(),
                        Matchers.anyListOf(String.class),
                        Matchers.anyListOf(String.class));
        doNothing().when(cloudFoundryClient).uploadApplication(Matchers.anyString(), anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getUris()).thenReturn(MutableList.of(DEFAULT_APPLICATION_DOMAIN));
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);

        CloudDomain cloudDomain = mock(CloudDomain.class);
        when(cloudDomain.getName()).thenReturn(DOMAIN);
        when(cloudFoundryClient.getSharedDomains()).thenReturn(MutableList.of(cloudDomain));

        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL);
        params.configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DOMAIN);
        params.configure(VanillaCloudfoundryApplication.BUILDPACK, Strings.makeRandomId(20));

        String applicationDomain = client.deploy(params.getAllConfig());

        assertEquals(applicationDomain, DEFAULT_APPLICATION_ADDRESS);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeployApplicationWithNonExistentDomain() throws IOException {
        doNothing().when(cloudFoundryClient).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyInt(),
                        Matchers.anyListOf(String.class),
                        Matchers.anyListOf(String.class));
        doNothing().when(cloudFoundryClient).uploadApplication(Matchers.anyString(), anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getUris()).thenReturn(MutableList.of(DEFAULT_APPLICATION_DOMAIN));
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);

        when(cloudFoundryClient.getDomains()).thenReturn(MutableList.<CloudDomain>of());

        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DOMAIN);

        String applicationDomain = client.deploy(params.getAllConfig());

        assertEquals(applicationDomain, DEFAULT_APPLICATION_ADDRESS);
    }

    @Test
    public void testDeployApplicationWithoutDomain() throws IOException {
        doNothing().when(cloudFoundryClient).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyInt(),
                        Matchers.anyListOf(String.class),
                        Matchers.anyListOf(String.class));
        doNothing().when(cloudFoundryClient).uploadApplication(Matchers.anyString(), anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getUris()).thenReturn(MutableList.of(DEFAULT_APPLICATION_DOMAIN));
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);

        CloudDomain cloudDomain = mock(CloudDomain.class);
        when(cloudDomain.getName()).thenReturn(DOMAIN);
        when(cloudFoundryClient.getDefaultDomain()).thenReturn(cloudDomain);
        when(cloudFoundryClient.getSharedDomains()).thenReturn(MutableList.of(cloudDomain));

        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL);
        params.configure(VanillaCloudfoundryApplication.BUILDPACK, Strings.makeRandomId(20));

        String applicationDomain = client.deploy(params.getAllConfig());

        assertEquals(applicationDomain, DEFAULT_APPLICATION_ADDRESS);
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testDeployNonExistentArtifact() throws IOException {
        doNothing().when(cloudFoundryClient).
                createApplication(
                        Matchers.anyString(),
                        Matchers.any(Staging.class),
                        Matchers.anyInt(),
                        Matchers.anyInt(),
                        Matchers.anyListOf(String.class),
                        Matchers.anyListOf(String.class));

        CloudDomain cloudDomain = mock(CloudDomain.class);
        when(cloudDomain.getName()).thenReturn(DOMAIN);
        when(cloudFoundryClient.getDefaultDomain()).thenReturn(cloudDomain);
        when(cloudFoundryClient.getSharedDomains()).thenReturn(MutableList.of(cloudDomain));

        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, Strings.makeRandomId(10));
        params.configure(VanillaCloudfoundryApplication.BUILDPACK, Strings.makeRandomId(20));

        doThrow(new PropagatedRuntimeException(new FileNotFoundException()))
                .when(cloudFoundryClient).uploadApplication(Matchers.anyString(), anyString());
        client.deploy(params.getAllConfig());
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testUpdateAnNotExistentArtifact() throws IOException {
        doThrow(new PropagatedRuntimeException(new FileNotFoundException()))
                .when(cloudFoundryClient).uploadApplication(Matchers.anyString(), anyString());
        client.pushArtifact(APPLICATION_NAME, Strings.makeRandomId(10));
    }

    @Test
    public void testStartApplication() {
        when(cloudFoundryClient.startApplication(anyString())).thenReturn(new StartingInfo(Strings.EMPTY));

        StartingInfo startingInfo = client.startApplication(APPLICATION_NAME);
        assertNotNull(startingInfo);
        assertEquals(startingInfo.getStagingFile(), Strings.EMPTY);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStartNonExistentApplication() {
        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(cloudFoundryClient).startApplication(Matchers.anyString());

        client.startApplication(APPLICATION_NAME);
    }

    @Test
    public void testGetApplicationStatus() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getState()).thenReturn(CloudApplication.AppState.STARTED);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);

        assertEquals(client.getApplicationStatus(APPLICATION_NAME), CloudApplication.AppState.STARTED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testGetStateNonExistentApplication() {
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(null);
        client.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testStopApplication() {
        doNothing().when(client).stopApplication(Matchers.anyString());

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);


        client.stopApplication(APPLICATION_NAME);
        CloudApplication.AppState state = client
                .getApplicationStatus(APPLICATION_NAME);
        assertEquals(state, CloudApplication.AppState.STOPPED);
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testStopNonExistentApplication() {
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(null);

        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(cloudFoundryClient).stopApplication(Matchers.anyString());

        client.stopApplication(APPLICATION_NAME);
    }

    @Test
    public void testDeleteApplication() {
        doNothing().when(cloudFoundryClient).deleteApplication(Matchers.anyString());
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(null);

        client.deleteApplication(APPLICATION_NAME);
        assertFalse(client.isDeployed(APPLICATION_NAME));
    }

    @Test(expectedExceptions = CloudFoundryException.class)
    public void testDeleteNonExistentApplication() {
        doThrow(new CloudFoundryException(HttpStatus.NOT_FOUND))
                .when(cloudFoundryClient).stopApplication(Matchers.anyString());

        client.getApplicationStatus(APPLICATION_NAME);
    }

    @Test
    public void testAddEnvToEmptyApplication() {
        Map<String, String> env = MutableMap.of("key1", "val1", "key2", "val2");
        testAdditionOfEnvToAnApplication(MutableMap.<String, String>of(), env);
    }

    @Test
    public void testAddNullEnvToEmptyApplication() {
        testAdditionOfEnvToAnApplication(MutableMap.<String, String>of(), null);
    }

    @Test
    public void testAddEnvToNotEmptyApplication() {
        Map<String, String> env = MutableMap.of("key1", "val1", "key2", "val2");
        testAdditionOfEnvToAnApplication(MutableMap.of("keyDefault1", "valueDefault1"), env);
    }

    private void testAdditionOfEnvToAnApplication(Map<String, String> applicationEnv,
                                                  Map<String, String> envToAdd) {

        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);

        final Map<String, String> mockApplicationEnv = applicationEnv;
        when(cloudApp.getEnvAsMap()).then(new Answer<Map<String, String>>() {
            @Override
            public Map<String, String> answer(InvocationOnMock invocation) throws Throwable {
                return mockApplicationEnv;
            }
        });

        doAnswer(new Answer() {
            @Override
            @SuppressWarnings("unchecked")
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Map<String, String> newEnv = (Map<String, String>) args[1];
                mockApplicationEnv.putAll(newEnv);
                return null;
            }
        }).when(cloudFoundryClient).updateApplicationEnv(anyString(),
                anyMapOf(String.class, String.class));

        assertEquals(client.getEnv(APPLICATION_NAME), applicationEnv);
        client.setEnv(APPLICATION_NAME, envToAdd);

        Map<String, String> returnedEnv = client.getEnv(APPLICATION_NAME);
        if (envToAdd != null) {
            applicationEnv.putAll(envToAdd);
        }
        assertEquals(returnedEnv, applicationEnv);
    }

    private ConfigBag getDefaultResourcesProfile() {
        ConfigBag params = new ConfigBag();
        params.configure(VanillaCloudfoundryApplication.REQUIRED_INSTANCES, INSTANCES);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_MEMORY, MEMORY);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_DISK, DISK);
        return params;
    }

    @Test
    public void testSetMemory() {
        client.setMemory(APPLICATION_NAME, MEMORY);
        verify(cloudFoundryClient, times(1)).updateApplicationMemory(APPLICATION_NAME, MEMORY);
        verifyNoMoreInteractions(cloudFoundryClient);
    }

    @Test
    public void testGetMemory() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getMemory()).thenReturn(MEMORY);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);
        assertEquals(client.getMemory(APPLICATION_NAME), MEMORY);
    }

    @Test
    public void testSetDiskQuota() {
        client.setDiskQuota(APPLICATION_NAME, DISK);
        verify(cloudFoundryClient, times(1)).updateApplicationDiskQuota(APPLICATION_NAME, DISK);
        verifyNoMoreInteractions(cloudFoundryClient);
    }

    @Test
    public void testGetDiskQuota() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getDiskQuota()).thenReturn(MEMORY);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);
        assertEquals(client.getDiskQuota(APPLICATION_NAME), MEMORY);
    }

    @Test
    public void testSetIntances() {
        client.setInstancesNumber(APPLICATION_NAME, INSTANCES);
        verify(cloudFoundryClient, times(1)).updateApplicationInstances(APPLICATION_NAME, INSTANCES);
        verifyNoMoreInteractions(cloudFoundryClient);
    }

    @Test
    public void testGetInstances() {
        CloudApplication cloudApp = mock(CloudApplication.class);
        when(cloudApp.getInstances()).thenReturn(INSTANCES);
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(cloudApp);
        assertEquals(client.getInstancesNumber(APPLICATION_NAME), INSTANCES);
    }
}
