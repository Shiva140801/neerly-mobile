package com.neerly.mobile.feature.customer;

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
public final class CustomerHomeViewModel_Factory implements Factory<CustomerHomeViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public CustomerHomeViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public CustomerHomeViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static CustomerHomeViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new CustomerHomeViewModel_Factory(repoProvider);
  }

  public static CustomerHomeViewModel newInstance(CustomerRepository repo) {
    return new CustomerHomeViewModel(repo);
  }
}
