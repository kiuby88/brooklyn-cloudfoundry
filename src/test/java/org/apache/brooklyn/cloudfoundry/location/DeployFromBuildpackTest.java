package org.apache.brooklyn.cloudfoundry.location;

import org.apache.brooklyn.util.core.config.ConfigBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class DeployFromBuildpackTest extends AbstractCloudFoundryLocationLiveTest {

    private static final Logger log = LoggerFactory.getLogger(DeployFromBuildpackTest.class);

    @Test(groups="Live")
    public void testDeployToOrgAndSpace() throws Exception {

        ConfigBag config = new ConfigBag();

        config.put(CloudFoundryPaasLocationConfig.CF_ORG, "org"); // org
        config.put(CloudFoundryPaasLocationConfig.CF_SPACE, "space"); // space

        String applicationName = cloudFoundryPaasLocation.deploy(config.getAllConfigAsConfigKeyMap());

        cloudFoundryPaasLocation.delete(applicationName);
    }

    // TODO add more tests with different combination
}
