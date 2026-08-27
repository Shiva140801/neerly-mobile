package com.neerly.mobile.feature.vendor.settings;

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
public final class VendorBusinessConfigViewModel_Factory implements Factory<VendorBusinessConfigViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorBusinessConfigViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorBusinessConfigViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorBusinessConfigViewModel_Factory create(
      Provider<VendorRepository> repoProvider) {
    return new VendorBusinessConfigViewModel_Factory(repoProvider);
  }

  public static VendorBusinessConfigViewModel newInstance(VendorRepository repo) {
    return new VendorBusinessConfigViewModel(repo);
  }
}
