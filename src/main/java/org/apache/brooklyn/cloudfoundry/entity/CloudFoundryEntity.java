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

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.BrooklynConfigKeys;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.time.Duration;

@ImplementedBy(CloudFoundryEntityImpl.class)
public interface CloudFoundryEntity extends Entity, Startable {

    @SetFromFlag("startTimeout")
    ConfigKey<Duration> START_TIMEOUT = BrooklynConfigKeys.START_TIMEOUT;

    AttributeSensor<CloudFoundryPaasLocation> CLOUDFOUNDRY_LOCATION = Sensors.newSensor(
            CloudFoundryPaasLocation.class, "cloudFoundryWebApp.paasLocation",
            "CloudFoundry location used to deploy the application");

    AttributeSensor<Boolean> SERVICE_PROCESS_IS_RUNNING = Sensors.newBooleanSensor(
            "service.process.isRunning",
            "Whether the process for the service is confirmed as running");

    AttributeSensor<Lifecycle> SERVICE_STATE_ACTUAL = Attributes.SERVICE_STATE_ACTUAL;

}