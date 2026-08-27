package com.neerly.mobile.feature.subscription;

import com.neerly.mobile.data.repo.CustomerRepository;
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
public final class SubscriptionListViewModel_Factory implements Factory<SubscriptionListViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public SubscriptionListViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public SubscriptionListViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static SubscriptionListViewModel_Factory create(
      Provider<CustomerRepository> repoProvider) {
    return new SubscriptionListViewModel_Factory(repoProvider);
  }

  public static SubscriptionListViewModel newInstance(CustomerRepository repo) {
    return new SubscriptionListViewModel(repo);
  }
}
