package com.neerly.mobile.feature.vendor.compliance;

import com.neerly.mobile.data.repo.VendorRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class VendorComplianceViewModel_Factory implements Factory<VendorComplianceViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorComplianceViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorComplianceViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorComplianceViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorComplianceViewModel_Factory(repoProvider);
  }

  public static VendorComplianceViewModel newInstance(VendorRepository repo) {
    return new VendorComplianceViewModel(repo);
  }
}
