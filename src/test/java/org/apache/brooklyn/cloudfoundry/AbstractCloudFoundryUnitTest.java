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
package org.apache.brooklyn.cloudfoundry;

import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.util.collections.MutableMap;
import org.testng.annotations.BeforeMethod;

public class AbstractCloudFoundryUnitTest extends BrooklynAppUnitTestSupport {

    protected static final String APPLICATION_NAME = UUID.randomUUID().toString().substring(0, 8);

    protected static final String APPLICATION_ARTIFACT_NAME =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    protected final String APPLICATION_ARTIFACT_URL =
            "classpath://" + APPLICATION_ARTIFACT_NAME;

    protected static final String DOMAIN = "brooklyndomain.io";
    protected static final String DEFAULT_APPLICATION_DOMAIN
            = APPLICATION_NAME + "." + DOMAIN;
    protected static final String DEFAULT_APPLICATION_ADDRESS
            = "https://" + DEFAULT_APPLICATION_DOMAIN;

    protected static final Map<String, String> EMPTY_ENV = MutableMap.of();
    protected static final Map<String, String> SIMPLE_ENV = MutableMap.of("k1", "v1");
    protected static final int MEMORY = 512;
    protected static final int INSTANCES = 1;
    protected static final int DISK = 1024;

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        mgmt = newLocalManagementContext();
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation();
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation() {
        Map<String, String> m = MutableMap.of();
        m.put("user", "super_user");
        m.put("password", "super_secret");
        m.put("org", "secret_organization");
        m.put("endpoint", "https://api.super.secret.io");
        m.put("space", "development");

        return (CloudFoundryPaasLocation)
                mgmt.getLocationRegistry().getLocationManaged("cloudfoundry", m);
    }

    protected LocalManagementContext newLocalManagementContext() {
        return new LocalManagementContextForTests(BrooklynProperties.Factory.newDefault());
    }

}