package com.neerly.mobile.feature.vendor.dashboard;

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
public final class VendorTodayViewModel_Factory implements Factory<VendorTodayViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorTodayViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorTodayViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorTodayViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorTodayViewModel_Factory(repoProvider);
  }

  public static VendorTodayViewModel newInstance(VendorRepository repo) {
    return new VendorTodayViewModel(repo);
  }
}
