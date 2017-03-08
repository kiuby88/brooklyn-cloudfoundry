package org.apache.brooklyn.cloudfoundry.entity;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.brooklyn.entity.software.base.EmptySoftwareProcessImpl;
import org.apache.brooklyn.feed.function.FunctionFeed;
import org.apache.brooklyn.feed.http.HttpFeed;
import org.apache.brooklyn.feed.ssh.SshFeed;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;

import com.google.common.collect.Maps;

public class CloudFoundryAppFromManifestImpl extends EmptySoftwareProcessImpl implements CloudFoundryAppFromManifest {

    private static final Duration FEED_UPDATE_PERIOD = Duration.seconds(30);

    private SshFeed sshFeed;
    private HttpFeed httpFeed;

    private FunctionFeed functionFeed;

    private Map<String, Object> lastCommandOutputs = Collections.synchronizedMap(Maps.<String, Object>newHashMapWithExpectedSize(3));

    private AtomicBoolean configurationChangeInProgress = new AtomicBoolean(false);

    @Override
    public void init() {
        super.init();
        checkConfiguration();
    }

    private void checkConfiguration() {
        String configurationUrl = getConfig(CONFIGURATION_URL);
        String configurationContents = getConfig(CONFIGURATION_CONTENTS);

        // Exactly one of the two must have a value
        if (Strings.isBlank(configurationUrl) == Strings.isBlank(configurationContents))
            throw new IllegalArgumentException("Exactly one of the two must have a value: '"
                    + CONFIGURATION_URL.getName() + "' or '" + CONFIGURATION_CONTENTS.getName() + "'.");
    }

    @Override
    public void rebind() {
        lastCommandOutputs = Collections.synchronizedMap(Maps.<String, Object>newHashMapWithExpectedSize(3));
        configurationChangeInProgress = new AtomicBoolean(false);

        super.rebind();
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();

//        Maybe<SshMachineLocation> machine = Locations.findUniqueSshMachineLocation(getLocations());
//        if (machine.isPresent()) {
//            addFeed(sshFeed = SshFeed.builder()
//                    .entity(this)
//                    .period(FEED_UPDATE_PERIOD)
//                    .machine(machine.get())
//                    .poll(new SshPollConfig<String>(SHOW)
//                            .command(getDriver().makeCloudFoundryCommand("show -no-color"))
//                            .onSuccess(new ShowSuccessFunction())
//                            .onFailure(new ShowFailureFunction()))
//                    .poll(new SshPollConfig<Map<String, Object>>(STATE)
//                            .command(getDriver().makeCloudFoundryCommand("refresh -no-color"))
//                            .onSuccess(new StateSuccessFunction())
//                            .onFailure(new StateFailureFunction()))
//                    .poll(new SshPollConfig<String>(PLAN)
//                            .command(getDriver().makeCloudFoundryCommand("plan -no-color"))
//                            .onSuccess(new PlanSuccessFunction())
//                            .onFailure(new PlanFailureFunction()))
//                    .poll(new SshPollConfig<String>(OUTPUT)
//                            .command(getDriver().makeCloudFoundryCommand("output -no-color --json"))
//                            .onSuccess(new OutputSuccessFunction())
//                            .onFailure(new OutputFailureFunction()))
//
//                    .build());
//        }

    }

    @Override
    protected void disconnectSensors() {
        disconnectServiceUpIsRunning();

        if (sshFeed != null) sshFeed.stop();
        if (functionFeed != null) functionFeed.stop();

        super.disconnectSensors();
    }

}
