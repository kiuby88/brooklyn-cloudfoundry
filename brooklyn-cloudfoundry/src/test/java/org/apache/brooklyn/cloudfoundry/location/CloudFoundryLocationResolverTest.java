package org.apache.brooklyn.cloudfoundry.location;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.test.BrooklynMgmtUnitTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CloudFoundryLocationResolverTest extends BrooklynMgmtUnitTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryLocationResolverTest.class);
    
    private BrooklynProperties brooklynProperties;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.setUp();
        brooklynProperties = mgmt.getBrooklynProperties();

        brooklynProperties.put("brooklyn.location.cloudfoundry.identity", "cloudfoundry-id");
        brooklynProperties.put("brooklyn.location.cloudfoundry.credential", "cloudfoundry-cred");
    }

    @Test
    public void testGivesCorrectLocationType() {
        LocationSpec<?> spec = getLocationSpec("cloudfoundry");
        assertEquals(spec.getType(), CloudFoundryLocation.class);

        CloudFoundryLocation loc = resolve("cloudfoundry");
        assertTrue(loc instanceof CloudFoundryLocation, "loc="+loc);
    }

    @Test
    public void testParametersInSpecString() {
        CloudFoundryLocation loc = resolve("cloudfoundry(endpoint=myMasterUrl)");
        assertEquals(loc.getConfig(CloudFoundryLocation.CLOUD_ENDPOINT), "myMasterUrl");
    }

    @Test
    public void testTakesDotSeparateProperty() {
        brooklynProperties.put("brooklyn.location.cloudfoundry.endpoint", "myMasterUrl");
        CloudFoundryLocation loc = resolve("cloudfoundry");
        assertEquals(loc.getConfig(CloudFoundryLocation.CLOUD_ENDPOINT), "myMasterUrl");
    }

    @Test
    public void testPropertiesPrecedence() {
        // prefer those in "spec" over everything else
        brooklynProperties.put("brooklyn.location.named.mycloudfoundry", "cloudfoundry:(loginUser=\"loginUser-inSpec\")");

        brooklynProperties.put("brooklyn.location.named.mycloudfoundry.loginUser", "loginUser-inNamed");
        brooklynProperties.put("brooklyn.location.cloudfoundry.loginUser", "loginUser-inDocker");

        // prefer those in "named" over everything else
        brooklynProperties.put("brooklyn.location.named.mycloudfoundry.privateKeyFile", "privateKeyFile-inNamed");
        brooklynProperties.put("brooklyn.location.cloudfoundry.privateKeyFile", "privateKeyFile-inDocker");

        // prefer those in cloudfoundry-specific
        brooklynProperties.put("brooklyn.location.cloudfoundry.publicKeyFile", "publicKeyFile-inDocker");

        Map<String, Object> conf = resolve("named:mycloudfoundry").config().getBag().getAllConfig();

        assertEquals(conf.get("loginUser"), "loginUser-inSpec");
        assertEquals(conf.get("privateKeyFile"), "privateKeyFile-inNamed");
        assertEquals(conf.get("publicKeyFile"), "publicKeyFile-inDocker");
    }

    private LocationSpec<?> getLocationSpec(String spec) {
        LOG.debug("Obtaining location spec '{}'", spec);
        return mgmt.getLocationRegistry().getLocationSpec(spec).get();
    }

    private CloudFoundryLocation resolve(String spec) {
        LOG.debug("Resolving location spec '{}'", spec);
        return (CloudFoundryLocation) mgmt.getLocationRegistry().getLocationManaged(spec);
    }
}
