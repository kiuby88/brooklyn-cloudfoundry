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
package org.apache.brooklyn.cloudfoundry.entity;


import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.CountdownTimer;
import org.apache.brooklyn.util.time.Duration;
import org.apache.brooklyn.util.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class VanillaCloudfoundryApplicationImpl extends AbstractEntity implements VanillaCloudfoundryApplication {

    private static final Logger log = LoggerFactory.getLogger(VanillaCloudfoundryApplicationImpl.class);
    private static final String DEFAULT_APP_PREFIX = "cf-app-";

    private CloudFoundryPaasLocation cfLocation;
    protected boolean connectedSensors = false;
    private String applicationName;
    private String applicationUrl;

    public VanillaCloudfoundryApplicationImpl() {
        super(MutableMap.of(), null);
    }

    public VanillaCloudfoundryApplicationImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public VanillaCloudfoundryApplicationImpl(Map properties) {
        this(properties, null);
    }

    public VanillaCloudfoundryApplicationImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    public void init() {
        super.init();
        initApplicationName();
    }

    @Override
    protected void initEnrichers() {
        super.initEnrichers();
        ServiceStateLogic.ServiceNotUpLogic
                .updateNotUpIndicator(this, SERVICE_PROCESS_IS_RUNNING,
                        "No information yet on whether this service is running");
    }

    private void initApplicationName() {
        applicationName = getConfig(APPLICATION_NAME);
        if (Strings.isBlank(applicationName)) {
            applicationName = DEFAULT_APP_PREFIX + Identifiers.makeRandomId(8);
        }
    }

    /**
     * If custom behaviour is required by sub-classes, consider overriding
     * {@link #doStart(java.util.Collection)})}.
     */
    @Override
    public final void start(final Collection<? extends Location> locations) {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStart(locations);
        } else {
            Task<?> task = Tasks.builder().name("start (sequential)").body(new Runnable() {
                public void run() {
                    doStart(locations);
                }
            }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * It is the first approach.
     * It does not start the entity children.
     */
    protected final void doStart(Collection<? extends Location> locations) {
        ServiceStateLogic.setExpectedState(this, Lifecycle.STARTING);
        try {
            preStart(locations);
            customStart();
            log.info("Entity {} was started", new Object[]{this});
            connectSensors();
            postStart();

            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
        } catch (Throwable t) {
            ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
            log.error("Error error starting entity {}", this);
            throw Exceptions.propagate(t);
        }
    }

    protected void preStart(Collection<? extends Location> locations) {
        this.addLocations(locations);
        if (getLocationOrNull() != null) {
            cfLocation = getLocationOrNull();
        } else {
            throw new ExceptionInInitializerError("Location should not be null in " + this +
                    " the entity needs a initialized Location");
        }
    }

    //Probably it would be better an Optional object
    private CloudFoundryPaasLocation getLocationOrNull() {
        return Iterables.get(Iterables
                .filter(getLocations(), CloudFoundryPaasLocation.class), 0, null);
    }

    protected void customStart() {
        deploy();
        preLaunch();
        launch();
        postLaunch();
    }

    private void deploy() {
        Map<String, Object> params = this.config().getBag().getAllConfig();
        params.put(APPLICATION_NAME.getName(), applicationName);
        applicationUrl = cfLocation.deploy(params);
    }

    private void preLaunch() {
        configureEnv();
    }

    private void configureEnv() {
        //TODO
        //Map<?, ?> envs = this.getConfig(VanillaCloudfoundryApplication.ENVS);
        //cfLocation.configureEnv(applicationName, (Map<Object, Object>) envs);
    }

    private void launch() {
        //TODO, starting the application
        cfLocation.startApplication(applicationName);
    }

    private void postLaunch() {
        waitForEntityStart();
        sensors().set(Attributes.MAIN_URI, URI.create(applicationUrl));
        sensors().set(VanillaCloudfoundryApplication.ROOT_URL, applicationUrl);
    }

    protected void postStart() {
        //waitEntityIsUp
    }

    protected void connectSensors() {
        connectedSensors = true;
        //TODO connectServiceIsRunning() and connectServiceUp()
    }

    @Override
    public void stop() {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStop();
        } else {
            Task<?> task = Tasks.builder().name("stop").body(new Runnable() {
                public void run() {
                    doStop();
                }
            }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * To be overridden instead of {@link #stop()}; sub-classes should call {@code super.doStop()}
     * and should add do additional work via tasks, executed using
     * {@link org.apache.brooklyn.util.core.task.DynamicTasks#queue(String, java.util.concurrent.Callable)}.
     */
    protected final void doStop() {

        log.info("Stopping {} in {}", new Object[]{this, getLocationOrNull()});

        if (getAttribute(SERVICE_STATE_ACTUAL)
                .equals(Lifecycle.STOPPED)) {
            log.warn("The entity {} is already stopped", new Object[]{this});
            return;
        }

        ServiceStateLogic.setExpectedState(this, Lifecycle.STOPPING);
        try {

            preStop();
            customStop();

            ServiceStateLogic.setExpectedState(this, Lifecycle.STOPPED);
            log.info("The entity stop operation {} is completed without errors",
                    new Object[]{this});
        } catch (Throwable t) {
            ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }

    protected void preStop() {
        this.sensors().set(SERVICE_UP, false);
        disconnectSensors();
    }

    /**
     * For disconnecting from the running service. Will be called on stop.
     */
    protected void disconnectSensors() {
        connectedSensors = false;
        //TODO disconnect
    }

    protected void customStop() {
        cfLocation.stop(applicationName);
    }

    @Override
    public void restart() {
        //TODO
    }

    @Override
    public void destroy() {
        super.destroy();
        disconnectSensors();
    }

    protected CloudFoundryPaasLocation getCloudFoundryLocation() {
        return cfLocation;
    }


    private void waitForEntityStart() {
        if (log.isDebugEnabled()) {
            log.debug("waiting to ensure {} doesn't abort prematurely", this);
        }
        Duration startTimeout = getConfig(START_TIMEOUT);
        CountdownTimer timer = startTimeout.countdownTimer();
        boolean isRunningResult = false;
        long delay = 100;
        while (!isRunningResult && !timer.isExpired()) {
            Time.sleep(delay);
            try {
                isRunningResult = isRunning();
            } catch (Exception e) {
                ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
                // provide extra context info, as we're seeing this happen in strange circumstances
                throw new IllegalStateException("Error detecting whether " + this +
                        " is running: " + e, e);
            }
            if (log.isDebugEnabled()) {
                log.debug("checked {}, is running returned: {}", this, isRunningResult);
            }
            // slow exponential delay -- 1.1^N means after 40 tries and 50s elapsed, it reaches
            // the max of 5s intervals
            // TODO use Repeater
            delay = Math.min(delay * 11 / 10, 5000);
        }
        if (!isRunningResult) {
            String msg = "Software process entity " + this + " did not pass is-running " +
                    "check within the required " + startTimeout + " limit (" +
                    timer.getDurationElapsed().toStringRounded() + " elapsed)";
            log.warn(msg + " (throwing)");
            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
            throw new IllegalStateException(msg);
        }
    }

    public boolean isRunning() {
        return isApplicationDomainAvailable();
    }

    protected boolean isApplicationDomainAvailable() {
        boolean result;
        try {
            result = HttpTool.getHttpStatusCode(applicationUrl) == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

}
