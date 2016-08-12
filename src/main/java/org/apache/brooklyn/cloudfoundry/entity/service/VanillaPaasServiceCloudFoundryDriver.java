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

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.entity.EntityPaasCloudFoundryDriver;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;

import com.google.common.annotations.Beta;

@Beta
public class VanillaPaasServiceCloudFoundryDriver extends EntityPaasCloudFoundryDriver
        implements VanillaPaasServiceDriver {

    private String serviceInstanceId;

    public VanillaPaasServiceCloudFoundryDriver(VanillaCloudFoundryServiceImpl entity,
                                                CloudFoundryPaasLocation location) {
        super(entity, location);
    }

    @Override
    public VanillaCloudFoundryServiceImpl getEntity() {
        return (VanillaCloudFoundryServiceImpl) super.getEntity();
    }

    @Override
    public boolean isRunning() {
        return getLocation().serviceInstanceExist(serviceInstanceId);
    }

    @Override
    public void rebind() {
        //TODO
    }

    @Override
    public void start() {
        createService();
    }

    private void createService() {
        Map<String, Object> params =
                MutableMap.copyOf(getEntity().config().getBag().getAllConfig());
        params.put(VanillaCloudFoundryService.SERVICE_INSTANCE_NAME.getName(),
                getEntity().getServiceInstanceName());
        serviceInstanceId = getEntity().getServiceInstanceName();
        getLocation().createServiceInstance(params);
    }

    @Override
    public void restart() {
        //TODO
    }

    @Override
    public void stop() {
        log.info("Service " + serviceInstanceId + " can only be started and deleted");
    }

    @Override
    public void delete() {
        unbindServiceOfApplications();
        getLocation().deleteServiceInstance(serviceInstanceId);
    }

    private void unbindServiceOfApplications() {
        List<String> boundApplications = getLocation().getBoundApplications(serviceInstanceId);
        if (boundApplications != null) {
            for (String boundApplication : boundApplications) {
                unbindServiceOfApplication(boundApplication);
            }
        }
    }

    private void unbindServiceOfApplication(String boundApplication) {
        try {
            getLocation().unbindService(serviceInstanceId, boundApplication);
        } catch (PropagatedRuntimeException e) {
            if (getLocation().isServiceBoundTo(serviceInstanceId, boundApplication)) {
                throw new PropagatedRuntimeException(e.getCause());
            }
            log.debug("Application is {} already unbound of {}",
                    boundApplication, serviceInstanceId);
        }
    }
}
