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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

public class VanillaPaasApplicationCloudFoundryDriverTest extends AbstractCloudFoundryUnitTest {

    @Mock
    VanillaCloudfoundryApplicationImpl entity;

    @Mock
    CloudFoundryPaasLocation location;

    private MockWebServer mockWebServer;
    private HttpUrl serverUrl;
    private String applicationUrl;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        mockWebServer = new MockWebServer();
        serverUrl = mockWebServer.url("/");
        applicationUrl = serverUrl.url().toString();

        mockWebServer.setDispatcher(getGenericDispatcher());
    }

    @Test
    public void testStartApplication() {
        when(location.deploy(anyMap())).thenReturn(applicationUrl);
        doNothing().when(location).startApplication(anyString());

        VanillaCloudfoundryApplicationImpl entity = new VanillaCloudfoundryApplicationImpl();
        assertNull(entity.getAttribute(Attributes.MAIN_URI));
        assertNull(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL));


        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, location);
        driver.start();
        assertEquals(entity.getAttribute(Attributes.MAIN_URI), serverUrl.uri());
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL), applicationUrl);
        assertTrue(driver.isRunning());
    }

    @Test
    public void testStopApplication() throws IOException {
        when(location.deploy(anyMap())).thenReturn(applicationUrl);
        doNothing().when(location).startApplication(anyString());
        doNothing().when(location).stop(anyString());

        VanillaCloudfoundryApplicationImpl entity = new VanillaCloudfoundryApplicationImpl();

        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, location);
        driver.start();
        assertTrue(driver.isRunning());

        driver.stop();
        mockWebServer.shutdown();
        assertFalse(driver.isRunning());
        verify(location, times(1)).stop(anyString());
    }

    @Test
    public void testDeleteApplication() throws IOException {
        VanillaCloudfoundryApplicationImpl entity = new VanillaCloudfoundryApplicationImpl();

        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, location);
        driver.delete();
        assertFalse(driver.isRunning());
        verify(location, times(1)).delete(anyString());
    }

    private Dispatcher getGenericDispatcher() {
        return new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

}
