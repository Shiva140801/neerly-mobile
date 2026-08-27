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
public final class VendorSettingsViewModel_Factory implements Factory<VendorSettingsViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorSettingsViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorSettingsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorSettingsViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorSettingsViewModel_Factory(repoProvider);
  }

  public static VendorSettingsViewModel newInstance(VendorRepository repo) {
    return new VendorSettingsViewModel(repo);
  }
}
