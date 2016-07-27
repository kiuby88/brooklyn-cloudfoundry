package org.apache.brooklyn.cloudfoundry.location;

import static org.testng.Assert.fail;

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.location.NoMachinesAvailableException;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@Test(groups = { "Live" })
public class CloudFoundryPaasLiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(CloudFoundryPaasLiveTest.class);

    private static final String USER = "root";
    private static final String PASSWORD = "password";

    private final String provider;

    protected ManagementContext managementContext;
    protected CloudFoundryPaasLocation location;
    protected List<String> apps = Lists.newArrayList();

    protected CloudFoundryPaasLiveTest() {
        this.provider = "cloudfoundry";
    }

    @DataProvider(name = "fromProviderId")
    public Object[][] providerIdAndGroupName() {
        return new Object[][] { {USER, PASSWORD } };
    }

    @BeforeMethod(alwaysRun=true)
    public void setUp() {
        managementContext = Entities.newManagementContext(ImmutableMap.of("provider", provider));
    }

    @Test(dataProvider = "fromProviderId")
    public void testPushingApp(String user, String password) {
        Map<ConfigKey<?>, ?> allConfig = MutableMap.<ConfigKey<?>, Object>builder()
                .put(CloudFoundryPaasLocation.ACCESS_IDENTITY, System.getProperty("test.cloudfoundry.identity"))
                .put(CloudFoundryPaasLocation.ACCESS_CREDENTIAL, System.getProperty("test.cloudfoundry.credential"))
                .build();

        LocationSpec<CloudFoundryPaasLocation> spec = LocationSpec.create(CloudFoundryPaasLocation.class).configure(allConfig);
        try {
            location = managementContext.getLocationManager().createLocation(spec);
        } catch (NullPointerException e) {
            throw new AssertionError("Failed to create " + CloudFoundryPaasLocation.class.getName());
        }
        String result;
        try {
            result = deployApplication(ImmutableMap.of("user", user, "password", password));
            LOG.info("Pushed app {}", result);
        } catch (NoMachinesAvailableException e) {
            fail();
        }
    }

    // Use this utility method to ensure machines are released on tearDown
    protected String deployApplication(Map<?, ?> flags) throws NoMachinesAvailableException {
        String app = location.deploy(flags);
        apps.add(app);
        return app;
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        List<Exception> exceptions = Lists.newArrayList();
        for(String app : apps) {
            try {
                location.delete(app);
            } catch (Exception e) {
                LOG.warn("Error deleting app {}; continuing...", app);
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
        apps.clear();
    }
}
