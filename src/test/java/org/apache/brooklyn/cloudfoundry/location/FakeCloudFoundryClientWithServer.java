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

import org.apache.brooklyn.util.collections.MutableList;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.Staging;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;


public class FakeCloudFoundryClientWithServer extends FakeCloudFoundryClient
        implements CloudFoundryOperations {

    private MockWebServer mockWebServer;
    private HttpUrl serverUrl;
    private String applicationBaseUrl;

    public FakeCloudFoundryClientWithServer() {
        super();
        mockWebServer = new MockWebServer();
        serverUrl = mockWebServer.url("/");
        applicationBaseUrl = serverUrl.url().toString();
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris, List<String> serviceNames) {
        this.createApplication(appName, staging, memory, uris, serviceNames);
        updateApplicationDiskQuota(appName, disk);
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        if ((uris == null) || (uris.size() != 1)) {
            throw new IllegalStateException("The application " + appName + " requieres at least " +
                    "an uri to be deployed");
        }
        String applicationPath = getApplicationUrl(uris.get(0));
        super.createApplication(appName, staging, memory, MutableList.of(applicationPath), serviceNames);
    }

    private String getApplicationUrl(String applicationName) {
        return applicationBaseUrl + applicationName;
    }

    private String getApplicationPathFromUrl(String applicationUrl) {
        return applicationUrl.replace(applicationBaseUrl, "");
    }

    @Override
    public StartingInfo startApplication(String appName) {
        CloudApplication application = getApplication(appName);
        String applicationPath = getApplicationPathFromUrl(application.getUris().get(0));
        mockWebServer.setDispatcher(enableApplicationDispatcher(applicationPath));
        return super.startApplication(appName);
    }

    @Override
    public void stopApplication(String appName) {
        CloudApplication application = getApplication(appName);
        String applicationPath = getApplicationPathFromUrl(application.getUris().get(0));
        mockWebServer.setDispatcher(disblaeApplicationDispatcher(applicationPath));
        super.stopApplication(appName);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String orgName) {
        return null;
    }

    private Dispatcher enableApplicationDispatcher(final String applicationDomain) {
        return new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/" + applicationDomain)) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private Dispatcher disblaeApplicationDispatcher(final String applicationDomain) {
        return new Dispatcher() {
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/" + applicationDomain)) {
                    return new MockResponse().setResponseCode(404);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    @Override
    public void deleteApplication(String appName) {
        stopApplication(appName);
        super.deleteApplication(appName);
    }

}
