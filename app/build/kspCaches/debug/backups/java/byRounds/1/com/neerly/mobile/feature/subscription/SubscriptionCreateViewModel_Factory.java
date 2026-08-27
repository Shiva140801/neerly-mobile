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
public final class SubscriptionCreateViewModel_Factory implements Factory<SubscriptionCreateViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public SubscriptionCreateViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public SubscriptionCreateViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static SubscriptionCreateViewModel_Factory create(
      Provider<CustomerRepository> repoProvider) {
    return new SubscriptionCreateViewModel_Factory(repoProvider);
  }

  public static SubscriptionCreateViewModel newInstance(CustomerRepository repo) {
    return new SubscriptionCreateViewModel(repo);
  }
}
