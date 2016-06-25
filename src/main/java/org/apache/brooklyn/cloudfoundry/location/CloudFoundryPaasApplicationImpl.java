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

public class CloudFoundryPaasApplicationImpl implements CloudFoundryPaasApplication {

    private final CloudFoundryPaasLocation platform;

    public CloudFoundryPaasApplicationImpl(CloudFoundryPaasLocation platform) {
        this.platform = platform;
    }

    public void init() {
        deployApplication();
    }

    private void deployApplication() {
        //TODO client.createApplication(...)
    }

    @Override
    public void start() {
        //TODO client...
    }

    @Override
    public void stop() {
        //TODO client...
    }

    @Override
    public void restart() {
        //TODO client...
    }
}
