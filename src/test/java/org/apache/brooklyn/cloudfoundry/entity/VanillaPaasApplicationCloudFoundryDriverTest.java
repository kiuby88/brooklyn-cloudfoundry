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
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.net.URI;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.util.text.Strings;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VanillaPaasApplicationCloudFoundryDriverTest extends AbstractCloudFoundryUnitTest {

    @Mock
    VanillaCloudfoundryApplicationImpl entity;

    @Mock
    CloudFoundryPaasLocation location;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStartApplication() {
        String applicationUrl = Strings.makeRandomId(20);
        when(location.deploy(anyMap())).thenReturn(applicationUrl);
        doNothing().when(location).startApplication(anyString());

        VanillaCloudfoundryApplicationImpl entity = new VanillaCloudfoundryApplicationImpl();
        assertNull(entity.getAttribute(Attributes.MAIN_URI));
        assertNull(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL));

        VanillaPaasApplicationDriver driver =
                new VanillaPaasApplicationCloudFoundryDriver(entity, location);
        driver.start();
        assertEquals(entity.getAttribute(Attributes.MAIN_URI), URI.create(applicationUrl));
        assertEquals(entity.getAttribute(VanillaCloudfoundryApplication.ROOT_URL), applicationUrl);
    }

}
