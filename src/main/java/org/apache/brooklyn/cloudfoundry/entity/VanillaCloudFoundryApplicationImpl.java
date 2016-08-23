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


import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaCloudFoundryApplicationImpl extends CloudFoundryEntityImpl
        implements VanillaCloudFoundryApplication {

    private static final Logger log = LoggerFactory.getLogger(VanillaCloudFoundryApplicationImpl.class);
    private static final String DEFAULT_APP_PREFIX = "cf-app-";

    private String applicationName;

    public VanillaCloudFoundryApplicationImpl() {
        super(MutableMap.of(), null);
    }

    public VanillaCloudFoundryApplicationImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public VanillaCloudFoundryApplicationImpl(Map properties) {
        this(properties, null);
    }

    public VanillaCloudFoundryApplicationImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    public void init() {
        super.init();
        initApplicationName();
    }

    private void initApplicationName() {
        applicationName = getConfig(APPLICATION_NAME);
        if (Strings.isBlank(applicationName)) {
            applicationName = DEFAULT_APP_PREFIX + Identifiers.makeRandomId(8);
        }
        this.sensors().set(APPLICATION_NAME, applicationName);
    }

    @Override
    public Class getDriverInterface() {
        return VanillaPaasApplicationDriver.class;
    }

    @Override
    public VanillaPaasApplicationDriver getDriver() {
        return (VanillaPaasApplicationDriver) super.getDriver();
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public void setEnv(String name, String value) {
        getDriver().setEnv(MutableMap.of(name, value));
    }

    @Override
    public void setInstancesNumber(int instancesNumber) {
        getDriver().setInstancesNumber(instancesNumber);
    }

    @Override
    public void setDiskQuota(int diskQuota) {
        getDriver().setDiskQuota(diskQuota);
    }

    @Override
    public void setMemory(int memory) {
        getDriver().setMemory(memory);
    }

}
