package com.neerly.mobile.feature.vendor.bank;

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
public final class VendorBankViewModel_Factory implements Factory<VendorBankViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorBankViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorBankViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorBankViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorBankViewModel_Factory(repoProvider);
  }

  public static VendorBankViewModel newInstance(VendorRepository repo) {
    return new VendorBankViewModel(repo);
  }
}
