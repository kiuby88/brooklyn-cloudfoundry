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

/*
public class CloudFoundryPaasClient {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPaasClient.class);


    private final CloudFoundryPaasLocation location;
    private CloudFoundryClient client;

    public CloudFoundryPaasClient(CloudFoundryPaasLocation location) {
        this.location = location;
    }

    protected CloudFoundryClient getClient() {
        if (client == null) {
            CloudCredentials credentials =
                    new CloudCredentials(
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_IDENTITY),
                            location.getConfig(CloudFoundryPaasLocationConfig.ACCESS_CREDENTIAL));
            client = new CloudFoundryClient(credentials,
                    getTargetURL(location.getConfig(CloudFoundryPaasLocationConfig.CLOUD_ENDPOINT)),
                    location.getConfig(CloudFoundryPaasLocationConfig.CF_ORG),
                    location.getConfig(CloudFoundryPaasLocationConfig.CF_SPACE), true);

            client.login();
        }
        return client;
    }

    public String deploy(Map<?, ?> params) {
        ConfigBag appSetUp = ConfigBag.newInstance(params);
        String artifactLocalPath = appSetUp.get(VanillaCloudFoundryApplication.ARTIFACT_PATH);
        String applicationName = appSetUp.get(VanillaCloudFoundryApplication.APPLICATION_NAME);

        getClient().createApplication(applicationName, getStaging(appSetUp),
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_DISK),
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_MEMORY),
                getUris(appSetUp), null);

        setInstancesNumber(applicationName,
                appSetUp.get(VanillaCloudFoundryApplication.REQUIRED_INSTANCES));

        pushArtifact(applicationName, artifactLocalPath);
        return getDomainUri(applicationName);
    }

    protected Staging getStaging(ConfigBag config) {
        String buildpack = config.get(VanillaCloudFoundryApplication.BUILDPACK);
        return new Staging(null, buildpack);
    }

    protected List<String> getUris(ConfigBag config) {
        return MutableList.of(inferApplicationRouteUri(config));
    }

    protected String inferApplicationRouteUri(ConfigBag config) {
        String domainId = config.get(VanillaCloudFoundryApplication.APPLICATION_DOMAIN);
        if (Strings.isBlank(domainId)) {
            domainId = getClient().getDefaultDomain().getName();
        }
        if (findSharedDomain(domainId) == null) {
            throw new RuntimeException("The target shared domain " + domainId + " does not exist");
        }

        String host = config.get(VanillaCloudFoundryApplication.APPLICATION_HOST);
        if (Strings.isBlank(host)) {
            host = config.get(VanillaCloudFoundryApplication.APPLICATION_NAME);
        }

        return host + "." + domainId;
    }

    private CloudDomain findSharedDomain(final String domainName) {
        return Iterables.find(getClient().getSharedDomains(), new Predicate<CloudDomain>() {
            @Override
            public boolean apply(CloudDomain domain) {
                return domainName.equals(domain.getName());
            }
        }, null);
    }

    private String getDomainUri(String applicationName) {
        String domainUri = null;
        Optional<CloudApplication> optional = getApplication(applicationName);
        if (optional.isPresent()) {
            domainUri = "https://" + optional.get().getUris().get(0);
        }
        return domainUri;
    }

    private Optional<CloudApplication> getApplication(String applicationName) {
        Optional<CloudApplication> app;
        try {
            app = Optional.fromNullable(getClient().getApplication(applicationName));
        } catch (CloudFoundryException e) {
            app = Optional.absent();
        }
        return app;
    }

    public void pushArtifact(String applicationName, String artifact) {
        try {
            getClient().uploadApplication(applicationName, artifact);
        } catch (IOException e) {
            log.error("Error updating the application artifact {} in {} ", artifact, this);
            throw Exceptions.propagate(e);
        }
    }

    public StartingInfo startApplication(String applicationName) {
        return getClient().startApplication(applicationName);
    }

    public void setEnv(String applicationName, Map<String, String> env) {
        if (env != null) {
            CloudApplication app = getClient().getApplication(applicationName);
            Map<String, String> oldEnv = app.getEnvAsMap();
            oldEnv.putAll(env);
            getClient().updateApplicationEnv(applicationName, oldEnv);
        }
    }

    public Map<String, String> getEnv(String applicationName) {
        return getClient().getApplication(applicationName).getEnvAsMap();
    }

    public void restartApplication(String applicationName) {
        getClient().restartApplication(applicationName);
    }

    public void stopApplication(String applicationName) {
        getClient().stopApplication(applicationName);
    }

    public void deleteApplication(String applicationName) {
        getClient().deleteApplication(applicationName);
    }

    public void restart(String applicationName) {
        //TODO
    }

    public CloudApplication.AppState getApplicationStatus(String applicationName) {
        Optional<CloudApplication> optional = getApplication(applicationName);
        if (optional.isPresent()) {
            return optional.get().getState();
        } else {
            throw Exceptions.propagate(new CloudFoundryException(HttpStatus.NOT_FOUND));
        }
    }

    public boolean isDeployed(String applicationName) {
        return getApplication(applicationName).isPresent();
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

    public void setInstancesNumber(String applicationName, int instancesNumber) {
        getClient().updateApplicationInstances(applicationName, instancesNumber);
    }

    public void setDiskQuota(String applicationName, int diskQuota) {
        getClient().updateApplicationDiskQuota(applicationName, diskQuota);
    }

    public void setMemory(String applicationName, int memory) {
        getClient().updateApplicationMemory(applicationName, memory);
    }

    public int getInstancesNumber(String applicationName) {
        return getClient().getApplication(applicationName).getInstances();
    }

    public int getDiskQuota(String applicationName) {
        return getClient().getApplication(applicationName).getDiskQuota();
    }

    public int getMemory(String applicationName) {
        return getClient().getApplication(applicationName).getMemory();
    }
}
*/
