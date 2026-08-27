package com.neerly.mobile.feature.vendor.catalog;

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
public final class VendorCatalogViewModel_Factory implements Factory<VendorCatalogViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorCatalogViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorCatalogViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorCatalogViewModel_Factory create(Provider<VendorRepository> repoProvider) {
    return new VendorCatalogViewModel_Factory(repoProvider);
  }

  public static VendorCatalogViewModel newInstance(VendorRepository repo) {
    return new VendorCatalogViewModel(repo);
  }
}
