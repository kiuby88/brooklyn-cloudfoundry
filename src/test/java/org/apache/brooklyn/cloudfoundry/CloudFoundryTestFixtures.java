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
package org.apache.brooklyn.cloudfoundry;

import java.util.Map;

import org.apache.brooklyn.util.collections.MutableMap;

public interface CloudFoundryTestFixtures {

    public static final int MEMORY = 512;
    public static final int INSTANCES = 1;
    public static final int DISK = 1024;
    public static final int CUSTOM_MEMORY = MEMORY * 2;
    public static final int CUSTOM_DISK = DISK * 2;
    public static final int CUSTOM_INSTANCES = INSTANCES * 2;
    public static final String APPLICATION_ARTIFACT =
            "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    public static final String ARTIFACT_URL = "classpath://" + APPLICATION_ARTIFACT;
    public static final Map<String, String> EMPTY_ENV = MutableMap.of();
    public static final Map<String, String> SIMPLE_ENV = MutableMap.of("k1", "v1");
    public static final String BROOKLYN_HOST = "test-brooklyn-host";

}
