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
package org.apache.brooklyn.cloudfoundry.entity.service.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import org.apache.brooklyn.cloudfoundry.entity.service.VanillaPaasServiceCloudFoundryDriver;
import org.apache.brooklyn.cloudfoundry.location.CloudFoundryPaasLocation;
import org.apache.brooklyn.util.core.ResourceUtils;

import com.google.common.annotations.Beta;

@Beta
public class PaasMySqlServiceCloudFoundryDriver extends VanillaPaasServiceCloudFoundryDriver
        implements PaasMySqlServiceDriver {

    private String serviceInstanceId;

    public PaasMySqlServiceCloudFoundryDriver(CloudFoundryMySqlServiceImpl entity,
                                              CloudFoundryPaasLocation location) {
        super(entity, location);
    }

    public CloudFoundryMySqlServiceImpl getEntity() {
        return (CloudFoundryMySqlServiceImpl) super.getEntity();
    }

    @Override
    public void operationAfterBindingTo(String applicationName) {
        Map<String, String> credentials = getCredentials(applicationName);
        String jdbcAddress = credentials.get(JDBC_URL_PROPERTY);
        initDatabase(jdbcAddress);
    }

    protected Map<String, String> getCredentials(String applicationName) {
        return getLocation().getCredentialsServiceForApplication(applicationName, serviceInstanceId);
    }

    private void initDatabase(String jdbcAddress) {
        //initDatabaseWithScript(jdbcAddress);
        getEntity().sensors()
                .set(CloudFoundryMySqlService.JDBC_ADDRESS, jdbcAddress);
    }

    //TODO: use this method
    private void initDatabaseWithScript(String jdbcAddress) {
        Connection con;
        Statement stmt;
        String DRIVER = "com.mysql.jdbc.Driver";
        try {
            Class.forName(DRIVER).newInstance();

            con = DriverManager.getConnection(jdbcAddress);
            stmt = con.createStatement();
            String sqlContent;

            sqlContent = getContentResourceFromUrl(getEntity().getCreationScriptUrl());

            stmt.execute(sqlContent);
            stmt.close();
            con.close();
        } catch (Exception e) {
            throw new RuntimeException("Error during database creation in driver" + this +
                    " deploying service " + getEntity().getId());
        }
    }

    private String getContentResourceFromUrl(String url) {
        return new ResourceUtils(getEntity())
                .getResourceAsString(url);
    }
}
