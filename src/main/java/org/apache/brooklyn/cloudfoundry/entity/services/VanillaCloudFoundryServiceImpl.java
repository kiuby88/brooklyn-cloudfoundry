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
package org.apache.brooklyn.cloudfoundry.entity.services;

import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryEntityImpl;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;

public class VanillaCloudFoundryServiceImpl extends CloudFoundryEntityImpl implements VanillaCloudFoundryService {

    private static final String DEFAULT_SERVICE_PREFIX = "cf-serv-";
    private String serviceInstanceName;

    public VanillaCloudFoundryServiceImpl() {
        super(MutableMap.of(), null);
    }

    public VanillaCloudFoundryServiceImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public VanillaCloudFoundryServiceImpl(Map properties) {
        this(properties, null);
    }

    public VanillaCloudFoundryServiceImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    public void init() {
        super.init();
        initServiceInstanceName();
    }

    private void initServiceInstanceName() {
        serviceInstanceName = getConfig(SERVICE_INSTANCE_NAME);
        if (Strings.isBlank(serviceInstanceName)) {
            serviceInstanceName = DEFAULT_SERVICE_PREFIX + Identifiers.makeRandomId(8);
        }
    }

    @Override
    public Class getDriverInterface() {
        return VanillaPaasServiceDriver.class;
    }

    @Override
    public VanillaPaasServiceDriver getDriver() {
        return (VanillaPaasServiceDriver) super.getDriver();
    }

    protected void connectSensors() {
        super.connectSensors();
        sensors().set(SERVICE_INSTANCE_ID, serviceInstanceName);
    }

    public String getServiceInstanceName() {
        return serviceInstanceName;
    }
}
