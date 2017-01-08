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

import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationResolverTest {

    private LocalManagementContext managementContext;
    private BrooklynProperties brooklynProperties;

    private final String MY_PAAS_LOCATION = "my-cloudfoundry";

    private static final String PROVIDER = "provider";
    private static final String IDENTITY = "user";
    private static final String CREDENTIAL = "password";
    private static final String ORG = "organization";
    private static final String SPACE = "space";
    private static final String ENDPOINT = "endpoint";
    private static final String ADDRESS = "run.provider.io";

    @BeforeMethod
    public void setUp() {
        managementContext = new LocalManagementContext(BrooklynProperties.Factory.newEmpty());
        brooklynProperties = managementContext.getBrooklynProperties();

        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION, "cloudfoundry");
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".provider", PROVIDER);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".identity", IDENTITY);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".credential", CREDENTIAL);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".org", ORG);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".endpoint", ENDPOINT);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".space", SPACE);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".address", ADDRESS);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (managementContext != null) {
            managementContext.terminate();
        }
    }

    @Test
    public void testCloudFoundryTakesProvidersScopedProperties() {
        CloudFoundryPaasLocation paasLocation = resolve(MY_PAAS_LOCATION);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.CLOUD_PROVIDER), PROVIDER);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.ACCESS_IDENTITY), IDENTITY);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.ACCESS_CREDENTIAL), CREDENTIAL);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.CLOUD_ENDPOINT), ENDPOINT);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.CF_ORG), ORG);
        assertEquals(paasLocation.getConfig(CloudFoundryPaasLocation.CF_SPACE), SPACE);
    }

    private CloudFoundryPaasLocation resolve(String spec) {
        return (CloudFoundryPaasLocation) managementContext.getLocationRegistry().getLocationManaged(spec);
    }

}
