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
package org.apache.brooklyn.cloudfoundry.location.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.util.core.flags.TypeCoercions;

import com.google.common.reflect.TypeToken;


public class VcapService {

    public static final String NAME = "name";
    public static final String CREDENTIALS = "credentials";
    public static final String LABEL = "label";
    public static final String TAGS = "tags";
    public static final String PLAN = "plan";


    private final String instanceName;
    private final Map<String, String> credentials;
    private final List<String> tags;
    private final String label;
    private final String plan;

    private VcapService(Builder builder) {
        this.instanceName = builder.name;
        this.credentials = builder.credentials;
        this.tags = builder.tags;
        this.label = builder.label;
        this.plan = builder.plan;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLabel() {
        return label;
    }

    public String getPlan() {
        return plan;
    }

    public static class Builder {
        private String name;
        private Map<String, String> credentials;
        private List<String> tags;
        private String label;
        private String plan;

        public Builder() {
        }

        public Builder name(String name) {
            this.name = checkNotNull(name, "name can not be null");
            return this;
        }

        public Builder credentials(Map<?, ?> credentials) {
            if (credentials != null) {
                this.credentials = TypeCoercions.coerce(credentials, new TypeToken<Map<String, String>>() {
                });
            }
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder tags(List<?> tags) {
            this.tags = TypeCoercions.coerce(tags, new TypeToken<List<String>>() {
            });
            return this;
        }

        public Builder plan(String plan) {
            this.plan = plan;
            return this;
        }

        public VcapService build() {
            return new VcapService(this);
        }

    }

}
