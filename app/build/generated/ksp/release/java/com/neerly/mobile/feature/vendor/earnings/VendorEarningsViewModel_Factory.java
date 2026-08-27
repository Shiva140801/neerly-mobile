package com.neerly.mobile.feature.vendor.earnings;

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
public final class VendorEarningsViewModel_Factory implements Factory<VendorEarningsViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorEarningsViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorEarningsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorEarningsViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorEarningsViewModel_Factory(repoProvider);
  }

  public static VendorEarningsViewModel newInstance(VendorRepository repo) {
    return new VendorEarningsViewModel(repo);
  }
}
