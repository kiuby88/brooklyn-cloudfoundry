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
import static org.testng.Assert.assertNotNull;

import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryPaasLocationTest extends BrooklynAppUnitTestSupport {

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @Mock
    protected CloudFoundryClient client;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation(client);
    }

    @Test
    public void testSetUpClient() {
        cloudFoundryPaasLocation.getClient();
        assertNotNull(cloudFoundryPaasLocation.getClient());
    }

    @Test
    public void testClientSingletonManagement() {
        CloudFoundryClient client1 = cloudFoundryPaasLocation.getClient();
        CloudFoundryClient client2 = cloudFoundryPaasLocation.getClient();
        assertNotNull(client1);
        assertEquals(client1, client2);
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation(CloudFoundryClient client) {
        return new CloudFoundryPaasLocation(client);
    }

}
