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
package org.apache.brooklyn.cloudfoundry.entity.service.mysql;


import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.cloudfoundry.entity.service.CloudFoundryService;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.apache.brooklyn.util.text.Strings;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CloudFoundryMySqlServiceTest extends AbstractCloudFoundryUnitTest {

    private CloudFoundryPaasLocation location;
    private static final String INIT_SCRIPT = "classpath://chat-database.sql";

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        location = spy(createCloudFoundryPaasLocation());
    }

    @Test
    public void testCreateService() throws IOException {
        doNothing().when(location).createServiceInstance(anyMap());
        doReturn(true).when(location).serviceInstanceExist(anyString());
        doReturn(getDefaultServiceCredentials())
                .when(location).getCredentialsServiceForApplication(anyString(), anyString());

        CloudFoundryMySqlService entity = addDefaultServiceToApp(INIT_SCRIPT);
        startServiceInLocationAndCheckSensors(entity, location);
        assertTrue(Strings
                .isNonBlank(entity.getAttribute(CloudFoundryService.SERVICE_INSTANCE_NAME)));

        doNothing().when(location).bindServiceToApplication(anyString(), anyString());
        entity.operationAfterBindingTo(APPLICATION_NAME);
        assertEquals(entity.getAttribute(CloudFoundryMySqlService.JDBC_ADDRESS), MOCK_JDBC_ADDRESS);
    }

    private ImmutableMap<String, String> getDefaultServiceCredentials() {
        return ImmutableMap.<String, String>builder()
                .put("jdbcUrl", MOCK_JDBC_ADDRESS)
                .put("uri", "mysql://host.net/ad?user=b0e8f")
                .put("name", "ad")
                .put("hostname", "host.net")
                .put("port", "3306")
                .put("username", "b0e8f")
                .put("password", "2876cd9e")
                .build();
    }

    protected CloudFoundryMySqlService addDefaultServiceToApp() {
        return addDefaultServiceToApp(Strings.EMPTY, Strings.EMPTY);
    }

    protected CloudFoundryMySqlService addDefaultServiceToApp(String serviceInstanceName,
                                                              String sqlScript) {
        return app.createAndManageChild(getServiceSpec(serviceInstanceName, sqlScript));
    }

    protected CloudFoundryMySqlService addDefaultServiceToApp(String sqlScript) {
        return app.createAndManageChild(getServiceSpec(Strings.EMPTY, sqlScript));
    }

    protected EntitySpec<CloudFoundryMySqlService> getServiceSpec(String serviceInstanceName, String sqlScript) {
        ConfigBag config = getServiceConfiguration(serviceInstanceName);
        config.configure(CloudFoundryMySqlService.CREATION_SCRIPT_TEMPLATE, sqlScript);

        return EntitySpec
                .create(CloudFoundryMySqlService.class)
                .configure(config.getAllConfigAsConfigKeyMap());
    }

}
