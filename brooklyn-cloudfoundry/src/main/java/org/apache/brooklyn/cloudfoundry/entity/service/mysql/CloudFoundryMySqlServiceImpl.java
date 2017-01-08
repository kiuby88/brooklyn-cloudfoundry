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

import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.cloudfoundry.entity.service.VanillaCloudFoundryServiceImpl;
import org.apache.brooklyn.util.collections.MutableMap;

import com.google.common.annotations.Beta;

@Beta
public class CloudFoundryMySqlServiceImpl extends VanillaCloudFoundryServiceImpl
        implements CloudFoundryMySqlService {

    public CloudFoundryMySqlServiceImpl() {
        super(MutableMap.of(), null);
    }

    public CloudFoundryMySqlServiceImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public CloudFoundryMySqlServiceImpl(Map properties) {
        this(properties, null);
    }

    public CloudFoundryMySqlServiceImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public Class getDriverInterface() {
        return PaasMySqlServiceDriver.class;
    }

    @Override
    public PaasMySqlServiceDriver getDriver() {
        return (PaasMySqlServiceDriver) super.getDriver();
    }

    @Override
    public void operationAfterBindingTo(String applicationName) {
        getDriver().operationAfterBindingTo(applicationName);
    }

    public String getCreationScriptUrl() {
        return getConfig(CREATION_SCRIPT_TEMPLATE);
    }
}
