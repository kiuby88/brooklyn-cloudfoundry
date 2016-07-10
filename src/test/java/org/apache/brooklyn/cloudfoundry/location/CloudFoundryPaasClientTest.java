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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudfoundryApplication;
import org.apache.brooklyn.util.collections.MutableList;
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
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasClientTest extends AbstractCloudFoundryUnitTest {

    protected static final String APPLICATION_NAME = UUID.randomUUID().toString().substring(0, 8);
    protected static final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    protected final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    private static final String DOMAIN = "brooklyndomain.io";
    private static final String DEFAULT_APPLICATION_DOMAIN
            = APPLICATION_NAME + "." + DOMAIN;
    private static final String DEFAULT_APPLICATION_ADDRESS
            = "https://" + DEFAULT_APPLICATION_DOMAIN;

    private static final int MEMORY = 512;
    private static final int INSTANCES = 1;
    private static final int DISK = 1024;

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

        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, APPLICATION_NAME);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, Strings.makeRandomId(10));
        params.configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DOMAIN);
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
    public void testGetStateUnexistentApplication() {
        when(cloudFoundryClient.getApplication(anyString())).thenReturn(null);

        client.getApplicationStatus(APPLICATION_NAME);
    }

    private ConfigBag getDefaultResourcesProfile() {
        ConfigBag params = new ConfigBag();
        params.configure(VanillaCloudfoundryApplication.REQUIRED_INSTANCES, INSTANCES);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_MEMORY, MEMORY);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_DISK, DISK);
        return params;
    }

}