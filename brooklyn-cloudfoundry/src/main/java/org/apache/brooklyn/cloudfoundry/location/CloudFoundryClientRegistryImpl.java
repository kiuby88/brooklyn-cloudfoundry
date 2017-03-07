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

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.brooklyn.cloudfoundry.suppliers.CloudFoundryClientSupplier;
import org.apache.brooklyn.cloudfoundry.suppliers.CloudFoundryOperationsSupplier;
import org.apache.brooklyn.cloudfoundry.suppliers.UaaClientSupplier;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.uaa.UaaClient;

public class CloudFoundryClientRegistryImpl implements CloudFoundryClientRegistry {

    public static final CloudFoundryClientRegistryImpl INSTANCE = new CloudFoundryClientRegistryImpl();

    private CloudFoundryClientSupplier cloudFoundryClientSupplier;
    private UaaClientSupplier uaaClientSupplier;
    private CloudFoundryOperationsSupplier cloudFoundryOperationsSupplier;

    protected CloudFoundryClientRegistryImpl() {
    }

    @Override
    public synchronized CloudFoundryClient getCloudFoundryClient(ConfigBag conf, boolean allowReuse) {
        if (cloudFoundryClientSupplier == null) {
            initCloudFoundryClientSupplier(conf);
        }
        return cloudFoundryClientSupplier.get();
    }

    @Override
    public synchronized UaaClient getUaaClient(ConfigBag conf, boolean allowReuse) {
        if (uaaClientSupplier == null) {
            initUaaClientSupplier(conf);
        }
        return uaaClientSupplier.get();
    }

    @Override
    public CloudFoundryOperations getCloudFoundryOperations(ConfigBag conf, boolean allowReuse) {
        if (cloudFoundryOperationsSupplier == null) {
            initCloudFoundryOperationsSupplier(conf);
        }
        return cloudFoundryOperationsSupplier.get();
    }

    private void initCloudFoundryClientSupplier(ConfigBag conf) {
        String apiHost = checkNotNull(conf.get(CloudFoundryLocationConfig.CLOUD_ENDPOINT), "endpoint must not be null");
        String user = checkNotNull(conf.get(CloudFoundryLocationConfig.ACCESS_IDENTITY), "identity must not be null");
        String password = checkNotNull(conf.get(CloudFoundryLocationConfig.ACCESS_CREDENTIAL), "credential must not be null");
        cloudFoundryClientSupplier = new CloudFoundryClientSupplier(apiHost, user, password);
    }

    private void initUaaClientSupplier(ConfigBag conf) {
        String apiHost = checkNotNull(conf.get(CloudFoundryLocationConfig.CLOUD_ENDPOINT), "endpoint must not be null");
        String user = checkNotNull(conf.get(CloudFoundryLocationConfig.ACCESS_IDENTITY), "identity must not be null");
        String password = checkNotNull(conf.get(CloudFoundryLocationConfig.ACCESS_CREDENTIAL), "credential must not be null");
        uaaClientSupplier = new UaaClientSupplier(apiHost, user, password);
    }

    private void initCloudFoundryOperationsSupplier(ConfigBag conf) {
        String organization = checkNotNull(conf.get(CloudFoundryLocationConfig.CF_ORG), "organization must not be null");
        String space = checkNotNull(conf.get(CloudFoundryLocationConfig.CF_SPACE), "space must not be null");
        cloudFoundryOperationsSupplier = new CloudFoundryOperationsSupplier(getCloudFoundryClient(conf, true), getUaaClient(conf, true), organization, space);
    }

}
