package org.apache.brooklyn.cloudfoundry.suppliers;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.uaa.UaaClient;

import com.google.common.base.Supplier;

public class CloudFoundryOperationsSupplier implements Supplier<CloudFoundryOperations> {

   private final CloudFoundryClient cloudFoundryClient;
   private final UaaClient uaaClient;
   private final String organization;
   private final String space;

   public CloudFoundryOperationsSupplier(CloudFoundryClient cloudFoundryClient, UaaClient uaaClient, String organization, String space) {
      this.cloudFoundryClient = cloudFoundryClient;
      this.uaaClient = uaaClient;
      this.organization = organization;
      this.space = space;
   }

   @Override
   public CloudFoundryOperations get() {

      return DefaultCloudFoundryOperations.builder()
              .cloudFoundryClient(cloudFoundryClient)
              .uaaClient(uaaClient)
              .organization(organization)
              .space(space)
              .build();
   }
}
