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

import org.apache.brooklyn.cloudfoundry.location.paas.PaasLocationConfig;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;

public interface CloudFoundryPaasLocationConfig extends PaasLocationConfig {

    ConfigKey<String> CF_ORG = ConfigKeys.newStringConfigKey("org",
            "Organization where paas resources will live.");

    ConfigKey<String> CF_SPACE = ConfigKeys.newStringConfigKey("space",
            "Space from the CloudFoundry services will be managed.");

    ConfigKey<CloudFoundryClientRegistry> CF_CLIENT_REGISTRY = ConfigKeys.newConfigKey(
            CloudFoundryClientRegistry.class, "cloudFoundryPaasClientRegistry",
            "Registry/Factory for creating cloudfoundry client; default is almost always fine, " +
                    "except where tests want to customize behaviour", CloudFoundryPaasClientRegistryImpl.INSTANCE);
}
