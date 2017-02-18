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
package org.apache.brooklyn.cloudfoundry.location;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.LocationConfigKeys;
import org.apache.brooklyn.core.location.cloud.CloudLocationConfig;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.time.Duration;

import com.google.common.base.Predicates;

public interface CloudFoundryLocationConfig extends CloudLocationConfig {

    @SetFromFlag("provider")
    ConfigKey<String> CLOUD_PROVIDER = LocationConfigKeys.CLOUD_PROVIDER;

    @SetFromFlag("identity")
    ConfigKey<String> ACCESS_IDENTITY = LocationConfigKeys.ACCESS_IDENTITY;

    @SetFromFlag("credential")
    ConfigKey<String> ACCESS_CREDENTIAL = LocationConfigKeys.ACCESS_CREDENTIAL;

    @SetFromFlag("endpoint")
    ConfigKey<String> CLOUD_ENDPOINT = LocationConfigKeys.CLOUD_ENDPOINT;

    ConfigKey<String> APPLICATION_NAME = ConfigKeys.builder(String.class)
            .name("application name")
            .description("CloudFoundry application name")
            .constraint(Predicates.<String>notNull())
            .build();
    
    ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org",
            "CloudFoundry Organization.");

    ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space",
            "CloudFoundry Space");

    ConfigKey<CloudFoundryClientRegistry> CF_CLIENT_REGISTRY = ConfigKeys.newConfigKey(
            CloudFoundryClientRegistry.class, "cloudFoundryClientRegistry",
            "Registry/Factory for creating cloudfoundry client; default is almost always fine, " +
                    "except where tests want to customize behaviour", CloudFoundryClientRegistryImpl.INSTANCE);
    
    ConfigKey<Duration> OPERATIONS_TIMEOUT = ConfigKeys.newConfigKey(Duration.class,
            "operations.timeout", "Timeout for cloudfoundry operations", Duration.minutes(5));


}
