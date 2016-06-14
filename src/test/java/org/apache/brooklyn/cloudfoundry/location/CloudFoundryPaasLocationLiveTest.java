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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.entity.utils.LocalResourcesDownloader;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.BrooklynAppLiveTestSupport;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.time.Duration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CloudFoundryPaasLocationLiveTest extends BrooklynAppLiveTestSupport {

    protected final String APPLICATION_NAME = "test-brooklyn-app" + UUID.randomUUID()
            .toString().substring(0, 8);

    protected static final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    protected final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    protected final String LOCATION_SPEC_NAME = "cloudfoundry-instance";
    protected final String JAVA_BUILDPACK = "https://github.com/cloudfoundry/java-buildpack.git";

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        mgmt = newLocalManagementContext();
        cloudFoundryPaasLocation = newSampleCloudFoundryLocationForTesting(LOCATION_SPEC_NAME);
    }

    @Test(groups = {"Live"})
    public void testClientSetUp() {
        cloudFoundryPaasLocation.setUpClient();
        assertNotNull(cloudFoundryPaasLocation.getClient());
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagement() throws Exception {
        cloudFoundryPaasLocation.setUpClient();
        File war;
        war = LocalResourcesDownloader
                .downloadResourceInLocalDir(APPLICATION_ARTIFACT_URL);
        String path = war.getCanonicalPath();
        String applicationDomain = cloudFoundryPaasLocation
                .deploy(APPLICATION_NAME, JAVA_BUILDPACK, path);

        startApplication(APPLICATION_NAME, applicationDomain);
        stopApplication(APPLICATION_NAME, applicationDomain);
        deleteApplicatin(APPLICATION_NAME);
    }

    private void startApplication(final String applicationName, final String applicationDomain) {
        cloudFoundryPaasLocation.startApplication(applicationName);

        Map<String, ?> flags = ImmutableMap.of("timeout", Duration.ONE_MINUTE);

        Asserts.succeedsEventually(flags, new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain), HttpURLConnection.HTTP_OK);
                    assertEquals(cloudFoundryPaasLocation.getApplicationStatus(applicationName),
                            CloudApplication.AppState.STARTED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void stopApplication(final String applicationName, final String applicationDomain) {
        cloudFoundryPaasLocation.stopApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain),
                            HttpURLConnection.HTTP_NOT_FOUND);
                    assertEquals(cloudFoundryPaasLocation.getApplicationStatus(APPLICATION_NAME),
                            CloudApplication.AppState.STOPPED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void deleteApplicatin(final String applicationName) {
        cloudFoundryPaasLocation.deleteApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertFalse(cloudFoundryPaasLocation.isDeployed(applicationName));
            }
        });
    }

    protected CloudFoundryPaasLocation newSampleCloudFoundryLocationForTesting(String spec) {
        return (CloudFoundryPaasLocation) mgmt.getLocationRegistry().resolve(spec);
    }

    protected LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests(BrooklynProperties.Factory.newDefault());
    }

    public String getClasspathUrlForResource(String resourceName) {
        return "classpath://" + resourceName;
    }
}
