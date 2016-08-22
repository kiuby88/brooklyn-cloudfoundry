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
package org.apache.brooklyn.cloudfoundry.entity.service;


import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.cloudfoundry.entity.CloudFoundryEntity;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.sensor.BasicAttributeSensorAndConfigKey;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

import com.google.common.annotations.Beta;

@Beta
@Catalog(name = "CloudFoundry Service")
@ImplementedBy(CloudFoundryServiceImpl.class)
public interface CloudFoundryService extends CloudFoundryEntity {

    @SetFromFlag("instanceName")
    BasicAttributeSensorAndConfigKey<String> SERVICE_INSTANCE_NAME =
            new BasicAttributeSensorAndConfigKey<String>(String.class,
                    "cloudFoundry.service.instance.name", "Given name for the service instance");

    @SetFromFlag("serviceName")
    ConfigKey<String> SERVICE_NAME = ConfigKeys.newStringConfigKey(
            "cloudFoundry.service.name", "Given name for the service instance");

    @SetFromFlag("plan")
    ConfigKey<String> PLAN = ConfigKeys.newStringConfigKey(
            "cloudFoundry.service.plan", "Selected plan for the service");
}
