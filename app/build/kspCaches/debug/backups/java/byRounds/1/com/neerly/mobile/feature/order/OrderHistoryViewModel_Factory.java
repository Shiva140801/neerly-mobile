package com.neerly.mobile.feature.order;

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
public final class OrderHistoryViewModel_Factory implements Factory<OrderHistoryViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public OrderHistoryViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public OrderHistoryViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static OrderHistoryViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new OrderHistoryViewModel_Factory(repoProvider);
  }

  public static OrderHistoryViewModel newInstance(CustomerRepository repo) {
    return new OrderHistoryViewModel(repo);
  }
}
