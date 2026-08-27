package com.neerly.mobile.feature.vendor;

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
public final class VendorOnboardingViewModel_Factory implements Factory<VendorOnboardingViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorOnboardingViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorOnboardingViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorOnboardingViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorOnboardingViewModel_Factory(repoProvider);
  }

  public static VendorOnboardingViewModel newInstance(VendorRepository repo) {
    return new VendorOnboardingViewModel(repo);
  }
}
