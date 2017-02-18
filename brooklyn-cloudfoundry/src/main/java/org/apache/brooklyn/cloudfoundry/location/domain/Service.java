/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.brooklyn.cloudfoundry.location.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

/**
 *    cf create-service SERVICE PLAN SERVICE_INSTANCE [-c PARAMETERS_AS_JSON] [-t TAGS]
 */
public class Service {

   private final String name;
   private final String plan;
   private final String serviceInstanceName;
   private final Map<String, String> parameters;
   private final List<String> tags;

   public Service(String name, String plan, String serviceInstanceName, Map<String, String> parameters, List<String> tags) {
      this.name = checkNotNull(name, "name");
      this.plan = checkNotNull(plan, "plan");
      this.serviceInstanceName = checkNotNull(serviceInstanceName, "serviceInstanceName");
      this.parameters = parameters;
      this.tags = tags;
   }

   public String getName() {
      return name;
   }

   public String getPlan() {
      return plan;
   }

   public String getServiceInstanceName() {
      return serviceInstanceName;
   }

   public Map<String, String> getParameters() {
      return parameters;
   }

   public List<String> getTags() {
      return tags;
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("Service{");
      sb.append("name='").append(name).append('\'');
      sb.append(", plan='").append(plan).append('\'');
      sb.append(", serviceInstanceName='").append(serviceInstanceName).append('\'');
      sb.append(", parameters=").append(parameters);
      sb.append(", tags=").append(tags);
      sb.append('}');
      return sb.toString();
   }
}
