package org.apache.brooklyn.cloudfoundry.entity;

import java.util.Map;

import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.sensor.BasicAttributeSensor;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

@ImplementedBy(CloudFoundryAppFromManifestImpl.class)
public interface CloudFoundryAppFromManifest extends SoftwareProcess {

    @SetFromFlag("cfManifestContents")
    ConfigKey<String> CONFIGURATION_CONTENTS = ConfigKeys.newStringConfigKey(
            "cf.manifest.contents",
            "Contents of the manifest file that will pushed to CloudFoundry.",
            "");

    @SetFromFlag("tfManifestUrl")
    ConfigKey<String> CONFIGURATION_URL = ConfigKeys.builder(String.class)
            .name("cf.manifest.url")
            .description("URL of the manifest file that will pushed to CloudFoundry.")
            .build();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    AttributeSensor<Map<String, Object>> STATE = new BasicAttributeSensor(Map.class, "tf.state",
            "A map constructed from the state file on disk which contains the state of all managed infrastructure.");

}
