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
package org.apache.brooklyn.cloudfoundry.suppliers;

import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;

import com.google.common.base.Supplier;

public class UaaClientSupplier implements Supplier<UaaClient> {

   private final String apiHost;
   private final String user;
   private final String password;

   public UaaClientSupplier(String apiHost, String user, String password) {
      this.apiHost = apiHost;
      this.user = user;
      this.password = password;
   }

   @Override
   public UaaClient get() {
      DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
              .apiHost(apiHost)
              .skipSslValidation(true) // TODO
              .build();
      PasswordGrantTokenProvider passwordGrantTokenProvider = PasswordGrantTokenProvider.builder()
              .username(user)
              .password(password)
              .build();
      return ReactorUaaClient.builder()
              .connectionContext(connectionContext)
              .tokenProvider(passwordGrantTokenProvider)
              .build();
   }
}
