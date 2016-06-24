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
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationResolverTest {

    private LocalManagementContext managementContext;
    private BrooklynProperties brooklynProperties;

    private final String MY_PAAS_LOCATION = "my-cloudfoundry";

    private final String IDENTITY = "user";
    private final String CREDENTIAL = "password";
    private final String ORG = "organization";
    private final String SPACE = "space";
    private final String ENDPOINT = "endpoint";
    private final String ADDRESS = "run.provider.io";

    @BeforeMethod
    public void setUp() {
        managementContext = new LocalManagementContext(BrooklynProperties.Factory.newEmpty());
        brooklynProperties = managementContext.getBrooklynProperties();

        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION, "cloudfoundry");
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".identity", IDENTITY);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".credential", CREDENTIAL);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".org", ORG);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".endpoint", ENDPOINT);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".space", SPACE);
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".address", ADDRESS);

        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".disk",
                CloudFoundryPaasLocationConfig.REQUIRED_DISK.getDefaultValue());
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".memory",
                CloudFoundryPaasLocationConfig.REQUIRED_MEMORY.getDefaultValue());
        brooklynProperties.put("brooklyn.location.named." + MY_PAAS_LOCATION + ".instances",
                CloudFoundryPaasLocationConfig.REQUIRED_INSTANCES.getDefaultValue());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (managementContext != null) {
            managementContext.terminate();
        }
    }

    @Test
    public void testCloudFoundryTakesProvidersScopedProperties() {
        ConfigBag config = resolve(MY_PAAS_LOCATION).config().getBag();
        assertEquals(config.get(CloudFoundryPaasLocation.ACCESS_IDENTITY), IDENTITY);
        assertEquals(config.get(CloudFoundryPaasLocation.ACCESS_CREDENTIAL), CREDENTIAL);
        assertEquals(config.get(CloudFoundryPaasLocation.CLOUD_ENDPOINT), ENDPOINT);
        assertEquals(config.get(CloudFoundryPaasLocation.CF_ORG), ORG);
        assertEquals(config.get(CloudFoundryPaasLocation.CF_SPACE), SPACE);

        assertEquals(config.get(CloudFoundryPaasLocationConfig.REQUIRED_DISK),
                CloudFoundryPaasLocationConfig.REQUIRED_DISK.getDefaultValue());
        assertEquals(config.get(CloudFoundryPaasLocationConfig.REQUIRED_MEMORY),
                CloudFoundryPaasLocationConfig.REQUIRED_MEMORY.getDefaultValue());
        assertEquals(config.get(CloudFoundryPaasLocationConfig.REQUIRED_INSTANCES),
                CloudFoundryPaasLocationConfig.REQUIRED_INSTANCES.getDefaultValue());
    }

    private CloudFoundryPaasLocation resolve(String spec) {
        return (CloudFoundryPaasLocation) managementContext.getLocationRegistry().resolve(spec);
    }

}
