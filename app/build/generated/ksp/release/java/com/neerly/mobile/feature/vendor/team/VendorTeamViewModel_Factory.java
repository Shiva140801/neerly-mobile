package com.neerly.mobile.feature.vendor.team;

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
public final class VendorTeamViewModel_Factory implements Factory<VendorTeamViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorTeamViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorTeamViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorTeamViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorTeamViewModel_Factory(repoProvider);
  }

  public static VendorTeamViewModel newInstance(VendorRepository repo) {
    return new VendorTeamViewModel(repo);
  }
}
