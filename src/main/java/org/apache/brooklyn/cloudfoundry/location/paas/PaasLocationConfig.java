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
package org.apache.brooklyn.cloudfoundry.location.paas;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.LocationConfigKeys;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

public interface PaasLocationConfig {

    @SetFromFlag("identity")
    ConfigKey<String> ACCESS_IDENTITY = LocationConfigKeys.ACCESS_IDENTITY;

    @SetFromFlag("credential")
    public static ConfigKey<String> ACCESS_CREDENTIAL = LocationConfigKeys.ACCESS_CREDENTIAL;

    @SetFromFlag("endpoint")
    public static ConfigKey<String> CLOUD_ENDPOINT = LocationConfigKeys.CLOUD_ENDPOINT;

    @SetFromFlag("paas.instances")
    ConfigKey<Integer> REQUIRED_INSTANCES = ConfigKeys.newIntegerConfigKey(
            "paas.profile.instances", "Number of instances of the application", 1);

    @SetFromFlag("paas.memory")
    ConfigKey<Integer> REQUIRED_MEMORY = ConfigKeys.newIntegerConfigKey(
            "paas.profile.memory", "Memory allocated for the application (MB)", 512);

    @SetFromFlag("paas.disk")
    ConfigKey<Integer> REQUIRED_DISK = ConfigKeys.newIntegerConfigKey(
            "paas.profile.disk", "Disk size allocated for the application (MB)", 1024);
}
