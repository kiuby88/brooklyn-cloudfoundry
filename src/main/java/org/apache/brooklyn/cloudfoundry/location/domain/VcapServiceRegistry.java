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
package org.apache.brooklyn.cloudfoundry.location.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.brooklyn.util.collections.MutableSet;
import org.apache.brooklyn.util.core.flags.TypeCoercions;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

public class VcapServiceRegistry {

    private final Multimap<String, VcapService> vcapServices;

    private VcapServiceRegistry(Multimap<String, VcapService> vcapServices) {
        this.vcapServices = vcapServices;
    }

    public Map<String, String> getCredentials(String instanceName) {
        Optional<VcapService> optional = tryFindVcapDescription(instanceName);
        if (optional.isPresent()) {
            return optional.get().getCredentials();
        }
        throw new IllegalArgumentException("Service instance " + instanceName
                + " was found in VCAP_SERVICES");
    }

    private Optional<VcapService> tryFindVcapDescription(String instanceName) {
        return Iterables.tryFind(vcapServices.values(), new Predicate<VcapService>() {
            @Override
            public boolean apply(VcapService input) {
                return instanceName.equals(input.getInstanceName());
            }
        });
    }

    public static VcapServiceRegistry createRegistryFromMap(Map<?, ?> params) {
        ListMultimap<String, VcapService> vcapServices = ArrayListMultimap.create();
        if (params != null) {
            for (Map.Entry<?, ?> entry : params.entrySet()) {
                String service = (String) entry.getKey();
                List<Map<?, ?>> serviceDescriptionMaps = TypeCoercions.coerce(entry.getValue(), new TypeToken<List<Map<?, ?>>>() {
                });
                vcapServices.putAll(service, createVcapServices(serviceDescriptionMaps));
            }
        }
        return new VcapServiceRegistry(vcapServices);
    }

    private static Collection<VcapService> createVcapServices(List<Map<?, ?>> serviceDescriptionMaps) {
        Set<VcapService> result = MutableSet.of();
        for (Map<?, ?> serviceDescription : serviceDescriptionMaps) {
            result.add(createVcapService(serviceDescription));
        }
        return result;
    }

    private static VcapService createVcapService(Map<?, ?> serviceDescriptionMap) {
        return new VcapService.Builder()
                .name((String) serviceDescriptionMap.get(VcapService.NAME))
                .plan((String) serviceDescriptionMap.get(VcapService.PLAN))
                .label((String) serviceDescriptionMap.get(VcapService.LABEL))
                .tags((List<?>) serviceDescriptionMap.get(VcapService.TAGS))
                .credentials((Map<?, ?>) serviceDescriptionMap.get(VcapService.CREDENTIALS))
                .build();
    }

}
