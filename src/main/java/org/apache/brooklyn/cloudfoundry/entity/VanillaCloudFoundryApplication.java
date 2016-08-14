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

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.annotation.Effector;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.sensor.BasicAttributeSensorAndConfigKey;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.text.Strings;

import com.google.common.reflect.TypeToken;

@Catalog(name = "Vanilla CloudFoundry Application entity")
@ImplementedBy(VanillaCloudFoundryApplicationImpl.class)
public interface VanillaCloudFoundryApplication extends CloudFoundryEntity {

    @SetFromFlag("nameApp")
    BasicAttributeSensorAndConfigKey<String> APPLICATION_NAME =
            new BasicAttributeSensorAndConfigKey<String>(String.class,
                    "cloudFoundry.application.name", "Name of the application");

    @SetFromFlag("path")
    ConfigKey<String> ARTIFACT_PATH = ConfigKeys.newStringConfigKey(
            "cloudFoundry.application.artifact", "URI of the application");

    @SetFromFlag("buildpack")
    ConfigKey<String> BUILDPACK = ConfigKeys.newStringConfigKey(
            "cloudFoundry.application.buildpack", "Buildpack to deploy an application");

    @SetFromFlag("env")
    @SuppressWarnings({"unchecked", "rawtypes"})
    BasicAttributeSensorAndConfigKey<Map<String, String>> ENV =
            new BasicAttributeSensorAndConfigKey(Map.class, "cloudFoundry.application.env",
                    "Environment variables for the application", MutableMap.<String, String>of());

    @SetFromFlag("domain")
    ConfigKey<String> APPLICATION_DOMAIN = ConfigKeys.newStringConfigKey(
            "cloudFoundry.application.domain", "Domain for the application", Strings.EMPTY);

    @SetFromFlag("host")
    ConfigKey<String> APPLICATION_HOST = ConfigKeys.newStringConfigKey(
            "cloudFoundry.application.host", "Host or sub-domain for the application, if " +
                    "this value is empty the application name will be used like the host");

    @SetFromFlag("services")
    ConfigKey<List<Object>> SERVICES = ConfigKeys.newConfigKey(new TypeToken<List<Object>>() {
    },
            "cloudFoundry.application.services", "Services to be bound", MutableList.<Object>of());

    @SetFromFlag("instances")
    ConfigKey<Integer> REQUIRED_INSTANCES = ConfigKeys.newIntegerConfigKey(
            "cloudfoundry.profile.instances", "Number of instances of the application", 1);

    @SetFromFlag("memory")
    ConfigKey<Integer> REQUIRED_MEMORY = ConfigKeys.newIntegerConfigKey(
            "cloudfoundry.profile.memory", "Memory allocated for the application (MB)", 512);

    @SetFromFlag("disk_quota")
    ConfigKey<Integer> REQUIRED_DISK = ConfigKeys.newIntegerConfigKey(
            "cloudfoundry.profile.disk", "Disk size allocated for the application (MB)", 1024);

    AttributeSensor<String> ROOT_URL =
            Sensors.newStringSensor("webapp.url", "URL of the application");

    AttributeSensor<Integer> INSTANCES =
            Sensors.newIntegerSensor("cloudfoundry.application.instances",
                    "Instances which are used to run the application");

    AttributeSensor<Integer> ALLOCATED_MEMORY =
            Sensors.newIntegerSensor("cloudfoundry.application.memory",
                    "Application allocated memory");

    AttributeSensor<Integer> ALLOCATED_DISK =
            Sensors.newIntegerSensor("cloudfoundry.application.disk", "Application allocated disk (MB)");

    @Effector(description = "Set an environment variable that can be retrieved by the web application")
    public void setEnv(@EffectorParam(name = "name", description = "Name of the variable") String name,
                       @EffectorParam(name = "value", description = "Value of the environment variable") String value);

    @Effector(description = "Set the desired number of instances that will be user by the web application")
    public void setInstancesNumber(@EffectorParam(name = "instancesNumber", description = "Number of " +
            "instance that are being used by the application") int instancesNumber);

    @Effector(description = "Set the desired disk quota that will be allocated")
    public void setDiskQuota(@EffectorParam(name = "diskQuota", description = "Disk allocated" +
            " that will be used by the web application") int diskQuota);

    @Effector(description = "Set the desired memory that will be allocated")
    public void setMemory(@EffectorParam(name = "memory", description = "Memory allocated") int memory);
}
