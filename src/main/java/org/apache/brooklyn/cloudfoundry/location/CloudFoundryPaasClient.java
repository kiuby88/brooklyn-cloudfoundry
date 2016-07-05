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

import java.io.IOException;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudFoundryPaasClient {

    private final CloudFoundryPaasLocation location;
    private CloudFoundryClient client;

    public CloudFoundryPaasClient(CloudFoundryPaasLocation location) {
        this.location = location;
        setUpClient();
    }

    private void setUpClient() {
        if (client == null) {
            CloudCredentials credentials =
                    new CloudCredentials(
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_IDENTITY),
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_CREDENTIAL));
            client = new CloudFoundryClient(credentials, null);
            client.login();
        }
    }

    public String deploy(Map<?, ?> params) {
        String domain;
        //TODO using client to deploy application
        //client.createApplication(applicationName, buildpack, memory, disk,...)
        //pushArtifact(applicationName, artifact);

        //returns the domain of the deployed application
        return null;
    }

    public void pushArtifact(String applicationName, String artifact) {
        //TODO
        try {

            client.uploadApplication(applicationName, artifact);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startApplication(String applicationName) {
        client.startApplication(applicationName);
    }

    public void setEnv(String applicationName, Map<Object, Object> envs) {
        //TODO
        client.getApplication(applicationName).setEnv(envs);
    }

    public void stop(String applicationName) {
        //TODO
        client.stopApplication(applicationName);
    }

}
