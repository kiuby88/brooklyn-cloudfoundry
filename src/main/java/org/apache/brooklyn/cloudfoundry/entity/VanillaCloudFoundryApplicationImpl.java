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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.drivers.EntityDriverManager;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.BrooklynConfigKeys;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.core.location.Locations;
import org.apache.brooklyn.feed.function.FunctionFeed;
import org.apache.brooklyn.feed.function.FunctionPollConfig;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.repeat.Repeater;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;

public class VanillaCloudFoundryApplicationImpl extends AbstractEntity implements VanillaCloudFoundryApplication {

    private static final Logger log = LoggerFactory.getLogger(VanillaCloudFoundryApplicationImpl.class);
    private static final String DEFAULT_APP_PREFIX = "cf-app-";

    private CloudFoundryPaasLocation cfLocation;
    private String applicationName;

    protected boolean connectedSensors = false;
    private FunctionFeed serviceProcessIsRunning;
    private FunctionFeed serviceProcessUp;
    private VanillaPaasApplicationDriver driver;

    public VanillaCloudFoundryApplicationImpl() {
        super(MutableMap.of(), null);
    }

    public VanillaCloudFoundryApplicationImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public VanillaCloudFoundryApplicationImpl(Map properties) {
        this(properties, null);
    }

    public VanillaCloudFoundryApplicationImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    public void init() {
        super.init();
        initApplicationName();
    }

    private void initApplicationName() {
        applicationName = getConfig(APPLICATION_NAME);
        if (Strings.isBlank(applicationName)) {
            applicationName = DEFAULT_APP_PREFIX + Identifiers.makeRandomId(8);
        }
        this.sensors().set(APPLICATION_NAME, applicationName);
    }

    @Override
    public Class getDriverInterface() {
        return VanillaPaasApplicationDriver.class;
    }

    @Override
    public VanillaPaasApplicationDriver getDriver() {
        return driver;
    }

    @Override
    protected void initEnrichers() {
        super.initEnrichers();
        ServiceStateLogic.ServiceNotUpLogic
                .updateNotUpIndicator(this, SERVICE_PROCESS_IS_RUNNING,
                        "No information yet on whether this service is running");
    }

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

    protected final void doStart(Collection<? extends Location> locations) {
        ServiceStateLogic.setExpectedState(this, Lifecycle.STARTING);
        try {
            preStart(findLocation(locations));
            customStart();
            log.info("Entity {} was started", new Object[]{this});
            postDriverStart();

            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
        } catch (Throwable t) {
            ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
            log.error("Error error starting entity {}", this);
            throw Exceptions.propagate(t);
        }
    }

    protected void preStart(Location location) {
        this.addLocations(MutableList.of(location));
        if (getLocationOrNull() != null) {
            cfLocation = getLocationOrNull();
        } else {
            throw new ExceptionInInitializerError("Location should not be null in " + this +
                    " the entity needs a initialized Location");
        }
        initDriver(cfLocation);
    }

    /*
     * TODO: avoiding boilerplate code
     * This method was gotten  getLocations in MachineLifecycleEffectorTasks
     */
    protected Location findLocation(@Nullable Collection<? extends Location> locations) {
        if (locations == null || locations.isEmpty()) {
            locations = this.getLocations();
        }

        locations = Locations.getLocationsCheckingAncestors(locations, this);

        if (locations.isEmpty())
            throw new IllegalArgumentException("No locations specified when starting " + this);
        if (locations.size() != 1 || Iterables.getOnlyElement(locations) == null)
            throw new IllegalArgumentException("Ambiguous locations detected when starting " + this + ": " + locations);
        return Iterables.getOnlyElement(locations);
    }

    //TODO: Probably it would be better an Optional object
    private CloudFoundryPaasLocation getLocationOrNull() {
        return Iterables.get(Iterables
                .filter(getLocations(), CloudFoundryPaasLocation.class), 0, null);
    }

    protected void customStart() {
        driver.start();
    }

    protected void postDriverStart() {
        waitForEntityStart();
        connectSensors();
        Entities.waitForServiceUp(this, Duration.of(
                getConfig(BrooklynConfigKeys.START_TIMEOUT).toMilliseconds(),
                TimeUnit.MILLISECONDS));
    }

    protected void connectSensors() {
        connectedSensors = true;
        connectServiceIsRunning();
        connectServiceUp();
    }

    protected void connectServiceIsRunning() {
        serviceProcessIsRunning = FunctionFeed.builder()
                .entity(this)
                .period(Duration.FIVE_SECONDS)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_PROCESS_IS_RUNNING)
                        .onException(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return driver.isRunning();
                            }
                        }))
                .build();
    }

    protected void connectServiceUp() {
        serviceProcessUp = FunctionFeed.builder()
                .entity(this)
                .period(Duration.FIVE_SECONDS)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_UP)
                        .onException(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return driver.isRunning();
                            }
                        }))
                .build();
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

    protected void customStop() {
        driver.stop();
        driver.delete();
    }

    protected void disconnectSensors() {
        connectedSensors = false;
        disconnectServiceIsRunning();
        disconnectServiceUp();
    }

    protected void disconnectServiceIsRunning() {
        if (serviceProcessIsRunning != null) {
            serviceProcessIsRunning.stop();
        }
        sensors().set(SERVICE_PROCESS_IS_RUNNING, null);
        sensors().remove(SERVICE_PROCESS_IS_RUNNING);
    }

    protected void disconnectServiceUp() {
        if (serviceProcessUp != null) {
            serviceProcessUp.stop();
        }
        sensors().set(SERVICE_UP, null);
        sensors().remove(SERVICE_UP);
    }

    @Override
    public void restart() {
        driver.restart();
    }

    @Override
    public void destroy() {
        super.destroy();
        disconnectSensors();
        driver.delete();
    }

    public void waitForEntityStart() {
        if (log.isDebugEnabled()) {
            log.debug("waiting to ensure {} doesn't abort prematurely", this);
        }
        Duration startTimeout = getConfig(START_TIMEOUT);
        boolean isRunningResult;

        isRunningResult = Repeater.create("Wait until the application is running")
                .until(new Callable<Boolean>() {
                    public Boolean call() {
                        return driver.isRunning();
                    }
                })
                .every(Duration.ONE_SECOND)
                .limitTimeTo(startTimeout)
                .run();

        if (!isRunningResult) {
            String msg = "Software process entity " + this + " did not pass is-running " +
                    "check within the required " + startTimeout;
            log.warn(msg + " (throwing)");
            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
            throw new IllegalStateException(msg);
        }
    }

    private void initDriver(CloudFoundryPaasLocation location) {
        VanillaPaasApplicationDriver newDriver = doInitDriver(location);
        if (newDriver == null) {
            throw new UnsupportedOperationException("cannot start " + this +
                    " on " + location + ": no driver available");
        }
        driver = newDriver;
    }

    private VanillaPaasApplicationDriver doInitDriver(CloudFoundryPaasLocation location) {
        if (driver != null) {
            if ((driver instanceof VanillaPaasApplicationCloudFoundryDriver)
                    && location.equals((driver).getLocation())) {
                return driver; //just reuse
            } else {
                log.warn("driver/location change is untested for {} at {}; changing driver and continuing", this, location);
                return newDriver(location);
            }
        } else {
            return newDriver(location);
        }
    }

    private VanillaPaasApplicationDriver newDriver(CloudFoundryPaasLocation location) {
        EntityDriverManager entityDriverManager = getManagementContext().getEntityDriverManager();
        return (VanillaPaasApplicationDriver) entityDriverManager.build(this, location);
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public void setEnv(String name, String value) {
        driver.setEnv(MutableMap.of(name, value));
    }

    @Override
    public void setInstancesNumber(int instancesNumber) {
        driver.setInstancesNumber(instancesNumber);
    }

    @Override
    public void setDiskQuota(int diskQuota) {
        driver.setDiskQuota(diskQuota);
    }

    @Override
    public void setMemory(int memory) {
        driver.setMemory(memory);
    }

}
