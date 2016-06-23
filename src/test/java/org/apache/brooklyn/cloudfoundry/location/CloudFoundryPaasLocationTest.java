package org.apache.brooklyn.cloudfoundry.location;

import java.util.Map;

import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.util.collections.MutableMap;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;

public class CloudFoundryPaasLocationTest extends BrooklynAppUnitTestSupport {

    protected CloudFoundryPaasLocation cloudFoundryPaasLocation;

    @Mock
    protected CloudFoundryClient client;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        cloudFoundryPaasLocation = createCloudFoundryPaasLocation();
    }

    private CloudFoundryPaasLocation createCloudFoundryPaasLocation() {
        Map<String, String> m = MutableMap.of();
        m.put("user", "super_user");
        m.put("password", "super_secret");
        m.put("org", "secret_organization");
        m.put("endpoint", "https://api.super.secret.io");
        m.put("space", "development");

        return (CloudFoundryPaasLocation)
                mgmt.getLocationRegistry().getLocationManaged("cloudfoundry", m);
    }
}
