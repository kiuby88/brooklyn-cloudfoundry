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

import java.util.Map;

import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.util.collections.MutableMap;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.common.collect.ImmutableMap;

public abstract class AbstractCloudFoundryLocationLiveTest {
    protected BrooklynProperties brooklynProperties;
    protected LocalManagementContext managementContext;
    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    protected CloudFoundryClientRegistry cloudFoundryPaasClientRegistry;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        managementContext = newLocalManagementContext();
        brooklynProperties = new LocalManagementContext().getBrooklynProperties();
        cloudFoundryPaasLocation = newSimpleCloudFoundryLocationForTesting(ImmutableMap.<ConfigKey<?>,Object>of());

        cloudFoundryPaasClientRegistry = new StubbedCloudFoundryPaasClientRegistry();
    }

    private LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests();
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (managementContext != null) Entities.destroyAll(managementContext);
    }

    private CloudFoundryPaasLocation newSimpleCloudFoundryLocationForTesting(Map<? extends ConfigKey<?>, ?> config) {
        String identity;
        String credential;
        identity = "user";
        credential = "password";
//        identity = (String) brooklynProperties.get("brooklyn.location.cloudfoundry.identity");
//        credential = (String) brooklynProperties.get("brooklyn.location.cloudfoundry.credential");

        Map<ConfigKey<?>, ?> allConfig = MutableMap.<ConfigKey<?>, Object>builder()
                .put(CloudFoundryPaasLocationConfig.CLOUD_ENDPOINT, "https://endpoint") // Location URL
                .put(CloudFoundryPaasLocationConfig.ACCESS_IDENTITY, identity) // Location username
                .put(CloudFoundryPaasLocationConfig.ACCESS_CREDENTIAL, credential) // Location password
                .put(CloudFoundryPaasLocationConfig.CF_CLIENT_REGISTRY, cloudFoundryPaasClientRegistry)
                .putAll(config)
                .build();

        LocationSpec<CloudFoundryPaasLocation> spec = LocationSpec.create(CloudFoundryPaasLocation.class).configure(allConfig);
        try {
            return managementContext.getLocationManager().createLocation(spec);
        } catch (NullPointerException e) {
            throw new AssertionError("Failed to create " + CloudFoundryPaasLocation.class.getName() +
                    ". Have you configured brooklyn.location.cloudfoundry.{identity,credential} in your " +
                    "brooklyn.properties file?");
        }
    }
}
