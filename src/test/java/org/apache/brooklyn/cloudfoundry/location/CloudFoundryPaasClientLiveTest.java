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

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.entity.VanillaCloudfoundryApplication;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.BrooklynAppLiveTestSupport;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CloudFoundryPaasClientLiveTest extends BrooklynAppLiveTestSupport {

    protected static final String APPLICATION_NAME_PREFIX = "test-brooklyn-app";

    protected static final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    protected final String APPLICATION_ARTIFACT_URL =
            getClasspathUrlForResource(APPLICATION_ARTIFACT_NAME);

    private static final String DEFAULT_DOMAIN = "cfapps.io";

    protected final String LOCATION_SPEC_NAME = "cloudfoundry-instance";
    protected final String JAVA_BUILDPACK = "https://github.com/cloudfoundry/java-buildpack.git";

    private static final int MEMORY = 512;
    private static final int INSTANCES = 1;
    private static final int DISK = 1024;

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;
    private CloudFoundryPaasClient cloudFoundryPaasClient;
    private String applicationName;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        mgmt = newLocalManagementContext();
        cloudFoundryPaasLocation = newSampleCloudFoundryLocationForTesting(LOCATION_SPEC_NAME);
        cloudFoundryPaasClient = new CloudFoundryPaasClient(cloudFoundryPaasLocation);

        applicationName = APPLICATION_NAME_PREFIX + UUID.randomUUID()
                .toString().substring(0, 8);
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagement() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL);
        params.configure(VanillaCloudfoundryApplication.APPLICATION_DOMAIN, DEFAULT_DOMAIN);
        params.configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    @Test(groups = {"Live"})
    public void testWebApplicationManagementWithoutDomain() throws Exception {
        ConfigBag params = getDefaultResourcesProfile();
        params.configure(VanillaCloudfoundryApplication.APPLICATION_NAME, applicationName);
        params.configure(VanillaCloudfoundryApplication.ARTIFACT_PATH, APPLICATION_ARTIFACT_URL);
        params.configure(VanillaCloudfoundryApplication.BUILDPACK, JAVA_BUILDPACK);

        applicationLifecycleManagement(applicationName, params.getAllConfig());
    }

    private void applicationLifecycleManagement(String applicationName, Map<String, Object> params) {
        String applicationUrl = cloudFoundryPaasClient.deploy(params);
        assertFalse(Strings.isBlank(applicationUrl));

        startApplication(applicationName, applicationUrl);
        stopApplication(applicationName, applicationUrl);
        deleteApplicatin(applicationName);
    }

    private void startApplication(final String applicationName, final String applicationDomain) {
        cloudFoundryPaasClient.startApplication(applicationName);

        Map<String, ?> flags = ImmutableMap.of("timeout", Duration.TWO_MINUTES);

        Asserts.succeedsEventually(flags, new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain), HttpURLConnection.HTTP_OK);
                    assertEquals(cloudFoundryPaasClient.getApplicationStatus(applicationName),
                            CloudApplication.AppState.STARTED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void stopApplication(final String applicationName, final String applicationDomain) {
        cloudFoundryPaasClient.stopApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                try {
                    assertEquals(HttpTool.getHttpStatusCode(applicationDomain),
                            HttpURLConnection.HTTP_NOT_FOUND);
                    assertEquals(cloudFoundryPaasClient.getApplicationStatus(applicationName),
                            CloudApplication.AppState.STOPPED);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });
    }

    private void deleteApplicatin(final String applicationName) {
        cloudFoundryPaasClient.deleteApplication(applicationName);
        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                assertFalse(cloudFoundryPaasClient.isDeployed(applicationName));
            }
        });
    }

    protected CloudFoundryPaasLocation newSampleCloudFoundryLocationForTesting(String spec) {
        return (CloudFoundryPaasLocation) mgmt.getLocationRegistry().resolve(spec);
    }

    protected LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests(BrooklynProperties.Factory.newDefault());
    }

    private ConfigBag getDefaultResourcesProfile() {
        ConfigBag params = new ConfigBag();
        params.configure(VanillaCloudfoundryApplication.REQUIRED_INSTANCES, INSTANCES);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_MEMORY, MEMORY);
        params.configure(VanillaCloudfoundryApplication.REQUIRED_DISK, DISK);
        return params;
    }

    public String getClasspathUrlForResource(String resourceName) {
        return "classpath://" + resourceName;
    }

}
