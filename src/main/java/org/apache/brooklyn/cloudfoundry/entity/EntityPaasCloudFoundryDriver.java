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


import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntityPaasCloudFoundryDriver implements EntityPaasDriver {

    public static final Logger log = LoggerFactory
            .getLogger(EntityPaasCloudFoundryDriver.class);

    private final CloudFoundryPaasLocation location;
    private CloudFoundryEntityImpl entity;

    public EntityPaasCloudFoundryDriver(CloudFoundryEntityImpl entity,
                                        CloudFoundryPaasLocation location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
    }

    @Override
    public CloudFoundryEntityImpl getEntity() {
        return entity;
    }

    @Override
    public CloudFoundryPaasLocation getLocation() {
        return location;
    }
}
