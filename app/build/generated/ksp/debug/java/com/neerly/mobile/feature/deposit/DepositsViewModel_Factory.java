package com.neerly.mobile.feature.deposit;

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
public final class DepositsViewModel_Factory implements Factory<DepositsViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public DepositsViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DepositsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DepositsViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new DepositsViewModel_Factory(repoProvider);
  }

  public static DepositsViewModel newInstance(CustomerRepository repo) {
    return new DepositsViewModel(repo);
  }
}
