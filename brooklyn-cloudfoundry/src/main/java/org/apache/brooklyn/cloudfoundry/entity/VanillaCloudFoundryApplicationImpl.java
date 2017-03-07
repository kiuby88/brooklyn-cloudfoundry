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


import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.entity.software.base.EmptySoftwareProcessImpl;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaCloudFoundryApplicationImpl extends EmptySoftwareProcessImpl implements VanillaCloudFoundryApplication {

    private static final Logger log = LoggerFactory.getLogger(VanillaCloudFoundryApplicationImpl.class);
    private static final String DEFAULT_APP_PREFIX = "cf-app-";

    private String applicationName;

    public void init() {
        super.init();
        //initApplicationName();
    }

    private void initApplicationName() {
        applicationName = getConfig(APPLICATION_NAME);
        if (Strings.isBlank(applicationName)) {
            applicationName = DEFAULT_APP_PREFIX + Identifiers.makeRandomId(8);
        }
        this.sensors().set(APPLICATION_NAME, applicationName);
    }

    @Override
    protected void disconnectSensors() {
        if(isSshMonitoringEnabled()) {
            disconnectServiceUpIsRunning();
        }
        super.disconnectSensors();
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        if (isSshMonitoringEnabled()) {
            connectServiceUpIsRunning();
        } else {
            sensors().set(Attributes.SERVICE_UP, true);
        }
    }

}
