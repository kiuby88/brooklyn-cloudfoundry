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

import static org.mockito.Mockito.spy;

import org.apache.brooklyn.util.core.config.ConfigBag;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;

public class StubbedCloudFoundryPaasClientRegistry implements CloudFoundryClientRegistry {

    private final FakeCloudFoundryOperations operations;
    private final CloudFoundryClient client;

    public StubbedCloudFoundryPaasClientRegistry() {
        operations = spy(new FakeCloudFoundryOperations());
        client = operations.getClient();
    }

    @Override
    public CloudFoundryOperations getCloudFoundryOperations(ConfigBag conf, boolean allowReuse) {
        return operations;
    }

    @Override
    public CloudFoundryClient getCloudFoundryClient(ConfigBag conf, boolean allowReuse) {
        return client;
    }
}
