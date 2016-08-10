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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.advanced.Advanced;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.buildpacks.Buildpacks;
import org.cloudfoundry.operations.domains.Domains;
import org.cloudfoundry.operations.organizationadmin.OrganizationAdmin;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.operations.serviceadmin.ServiceAdmin;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.spaceadmin.SpaceAdmin;
import org.cloudfoundry.operations.spaces.Spaces;
import org.cloudfoundry.operations.stacks.Stacks;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import reactor.core.publisher.Mono;


public class FakeCloudFoundryOperations implements CloudFoundryOperations {

    private FakeApplications applications;
    private CloudFoundryClient client;

    public FakeCloudFoundryOperations() {
        applications = new FakeApplications();
        init();
    }

    protected void init() {
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        when(applicationsV2.update(any(UpdateApplicationRequest.class)))
                .thenAnswer(new Answer<Mono<UpdateApplicationResponse>>() {
                    @Override
                    public Mono<UpdateApplicationResponse> answer(InvocationOnMock invocation) throws Throwable {
                        UpdateApplicationRequest request =
                                (UpdateApplicationRequest) invocation.getArguments()[0];
                        return applications.updateApplication(request);
                    }
                });
        client = mock(CloudFoundryClient.class);
        when(client.applicationsV2()).thenReturn(applicationsV2);
    }

    @Override
    public Applications applications() {
        return applications;
    }

    @Override
    public Advanced advanced() {
        return null;
    }

    @Override
    public Buildpacks buildpacks() {
        return null;
    }

    @Override
    public Domains domains() {
        return null;
    }

    @Override
    public OrganizationAdmin organizationAdmin() {
        return null;
    }

    @Override
    public Organizations organizations() {
        return null;
    }

    @Override
    public Routes routes() {
        return null;
    }

    @Override
    public ServiceAdmin serviceAdmin() {
        return null;
    }

    @Override
    public Services services() {
        return null;
    }

    @Override
    public SpaceAdmin spaceAdmin() {
        return null;
    }

    @Override
    public Spaces spaces() {
        return null;
    }

    @Override
    public Stacks stacks() {
        return null;
    }

    public CloudFoundryClient getClient() {
        return client;
    }

}
