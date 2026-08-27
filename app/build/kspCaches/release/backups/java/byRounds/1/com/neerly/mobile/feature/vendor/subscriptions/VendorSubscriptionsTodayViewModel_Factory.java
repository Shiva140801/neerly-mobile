package com.neerly.mobile.feature.vendor.subscriptions;

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
public final class VendorSubscriptionsTodayViewModel_Factory implements Factory<VendorSubscriptionsTodayViewModel> {
  private final Provider<VendorRepository> repoProvider;

  public VendorSubscriptionsTodayViewModel_Factory(Provider<VendorRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public VendorSubscriptionsTodayViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static VendorSubscriptionsTodayViewModel_Factory create(
      Provider<VendorRepository> repoProvider) {
    return new VendorSubscriptionsTodayViewModel_Factory(repoProvider);
  }

  public static VendorSubscriptionsTodayViewModel newInstance(VendorRepository repo) {
    return new VendorSubscriptionsTodayViewModel(repo);
  }
}
