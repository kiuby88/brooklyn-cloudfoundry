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


public class FakeCloudFoundryClient implements CloudFoundryOperations {

    private Applications applications;

    public FakeCloudFoundryClient() {
        applications = new FakeApplications();
    }

    @Override
    public Advanced advanced() {
        return null;
    }

    @Override
    public Applications applications() {
        return applications;
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

}
