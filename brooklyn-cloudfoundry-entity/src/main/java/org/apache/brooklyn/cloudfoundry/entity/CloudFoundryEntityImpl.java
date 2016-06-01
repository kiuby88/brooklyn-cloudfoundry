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

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.feed.function.FunctionFeed;
import org.apache.brooklyn.feed.function.FunctionPollConfig;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.time.CountdownTimer;
import org.apache.brooklyn.util.time.Duration;
import org.apache.brooklyn.util.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;

public abstract class CloudFoundryEntityImpl extends AbstractEntity implements CloudFoundryEntity {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryEntityImpl.class);

    /**
     * @see #connectServiceUpIsRunning()
     */
    private volatile FunctionFeed serviceProcessIsRunning;
    protected boolean connectedSensors = false;
    private CloudFoundryPaasLocation cfLocation;

    public CloudFoundryEntityImpl() {
        super(MutableMap.of(), null);
    }

    public CloudFoundryEntityImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public CloudFoundryEntityImpl(Map properties) {
        this(properties, null);
    }

    public CloudFoundryEntityImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    public void init() {
        super.init();
    }

    @Override
    protected void initEnrichers() {
        super.initEnrichers();
        ServiceStateLogic.ServiceNotUpLogic
                .updateNotUpIndicator(this, SERVICE_PROCESS_IS_RUNNING,
                        "No information yet on whether this service is running");
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
            cfLocation.setUpClient();
        } else {
            throw new ExceptionInInitializerError("Location should not be null in " + this +
                    " the entity needs a initialized Location");
        }
    }

    protected abstract void customStart();

    protected void postStart() {
        //waitEntityIsUp
    }

    @Override
    public void stop() {
        //TODO
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

    /**
     * For binding to the running service (e.g. connecting sensors to registry). Will be called
     * on start() and on rebind().
     * <p/>
     * Implementations should be idempotent (ie tell whether sensors already connected),
     * though the framework is pretty good about not calling when already connected.
     */
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
                                return isRunning();
                            }
                        }))
                .build();
    }

    protected void connectServiceUp() {
        serviceProcessIsRunning = FunctionFeed.builder()
                .entity(this)
                .period(Duration.FIVE_SECONDS)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_UP)
                        .onException(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return isRunning();
                            }
                        }))
                .build();
    }

    protected abstract boolean isRunning();

    /**
     * For disconnecting from the running service. Will be called on stop.
     */
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
        if (serviceProcessIsRunning != null) {
            serviceProcessIsRunning.stop();
        }
        sensors().set(SERVICE_UP, null);
        sensors().remove(SERVICE_UP);
    }

    /**
     * Copied direcly from {@link org.apache.brooklyn.entity.software.base.SoftwareProcessImpl}
     */
    // TODO Find a better way to detect early death of process.
    public void waitForEntityStart() {
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

    //Probably it would be better an Optional object
    private CloudFoundryPaasLocation getLocationOrNull() {
        return Iterables.get(Iterables
                .filter(getLocations(), CloudFoundryPaasLocation.class), 0, null);
    }

    protected CloudFoundryPaasLocation getCloudFoundryLocation() {
        return cfLocation;
    }

}
