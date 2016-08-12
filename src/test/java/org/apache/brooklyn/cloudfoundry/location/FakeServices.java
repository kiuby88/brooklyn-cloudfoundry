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


import java.util.List;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.AbstractCloudFoundryUnitTest;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.text.Strings;
import org.cloudfoundry.client.v2.CloudFoundryException;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceKeyRequest;
import org.cloudfoundry.operations.services.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.ListServiceOfferingsRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.ServiceOffering;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateUserProvidedServiceInstanceRequest;

import com.google.common.collect.ImmutableList;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FakeServices implements Services {

    FakeApplications applications;
    Map<String, List<String>> availableServices;
    Map<String, ServiceInstance> services;

    public FakeServices(FakeApplications applications) {
        this.applications = applications;
        services = MutableMap.of();
        availableServices = MutableMap.of();
        availableServices
                .put(AbstractCloudFoundryUnitTest.SERVICE_X,
                        ImmutableList.of(AbstractCloudFoundryUnitTest.SERVICE_X_PLAN));
    }

    @Override
    public Mono<Void> bind(BindServiceInstanceRequest bindServiceInstanceRequest) {
        String instanceName = bindServiceInstanceRequest.getServiceInstanceName();
        String applicationName = bindServiceInstanceRequest.getApplicationName();

        if (!services.containsKey(instanceName)) {
            throwNonExistentService(instanceName);
        }
        if (!applications.containsApplication(applicationName)) {
            throwNonExistentApplication(applicationName);
        }

        ServiceInstance service = services.get(instanceName);
        services.put(instanceName, ServiceInstance.builder()
                .from(service)
                .application(applicationName)
                .build());
        return Mono.empty();
    }

    @Override
    public Mono<Void> createInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        String instanceName = createServiceInstanceRequest.getServiceInstanceName();
        if (!services.containsKey(instanceName)) {
            checkServiceAndPlan(createServiceInstanceRequest.getServiceName(),
                    createServiceInstanceRequest.getPlanName());
            ServiceInstance service = ServiceInstance.builder()
                    .service(createServiceInstanceRequest.getServiceName())
                    .name(instanceName)
                    .plan(createServiceInstanceRequest.getPlanName())
                    .type(ServiceInstanceType.MANAGED)
                    .id(Strings.makeRandomId(10))
                    .build();
            services.put(instanceName, service);
            return Mono.empty();
        }
        throw new CloudFoundryException(60002, "The service instance name is taken: " + instanceName,
                "CF-ServiceInstanceNameTaken");
    }

    private void checkServiceAndPlan(String service, String plan) {
        if (!availableServices.containsKey(service)) {
            throwNonExistentService(service);
        }
        if (!availableServices.get(service).contains(plan)) {
            throwNonExistentPlan(plan);
        }
    }

    private void throwNonExistentService(String serviceName) {
        throw new IllegalArgumentException("Service " + serviceName + " does not exist");
    }

    private void throwNonExistentPlan(String plan) {
        throw new IllegalArgumentException("Service plan " + plan + " does not exist");
    }

    private void throwNonExistentApplication(String applicationName) {
        throw new IllegalArgumentException("Application " + applicationName + "does not exist");
    }

    @Override
    public Mono<Void> createServiceKey(CreateServiceKeyRequest createServiceKeyRequest) {
        return null;
    }

    @Override
    public Mono<Void> createUserProvidedInstance(CreateUserProvidedServiceInstanceRequest createUserProvidedServiceInstanceRequest) {
        return null;
    }

    @Override
    public Mono<Void> deleteInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) {
        return null;
    }

    @Override
    public Mono<Void> deleteServiceKey(DeleteServiceKeyRequest deleteServiceKeyRequest) {
        return null;
    }

    @Override
    public Mono<ServiceInstance> getInstance(GetServiceInstanceRequest getServiceInstanceRequest) {
        String instanceName = getServiceInstanceRequest.getName();
        if (services.containsKey(instanceName)) {
            return Mono.just(services.get(instanceName));
        }
        throw new IllegalArgumentException("Service instance " + instanceName + " does not exist");
    }

    @Override
    public Mono<ServiceKey> getServiceKey(GetServiceKeyRequest getServiceKeyRequest) {
        return null;
    }

    @Override
    public Flux<ServiceInstance> listInstances() {
        return null;
    }

    @Override
    public Flux<ServiceKey> listServiceKeys(ListServiceKeysRequest listServiceKeysRequest) {
        return null;
    }

    @Override
    public Flux<ServiceOffering> listServiceOfferings(ListServiceOfferingsRequest listServiceOfferingsRequest) {
        return null;
    }

    @Override
    public Mono<Void> renameInstance(RenameServiceInstanceRequest renameServiceInstanceRequest) {
        return null;
    }

    @Override
    public Mono<Void> unbind(UnbindServiceInstanceRequest unbindServiceInstanceRequest) {
        return null;
    }

    @Override
    public Mono<Void> updateInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest) {
        return null;
    }

    @Override
    public Mono<Void> updateUserProvidedInstance(UpdateUserProvidedServiceInstanceRequest updateUserProvidedServiceInstanceRequest) {
        return null;
    }
}
