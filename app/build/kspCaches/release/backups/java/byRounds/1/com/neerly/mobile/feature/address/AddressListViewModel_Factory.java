package com.neerly.mobile.feature.address;

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
public final class AddressListViewModel_Factory implements Factory<AddressListViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public AddressListViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public AddressListViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static AddressListViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new AddressListViewModel_Factory(repoProvider);
  }

  public static AddressListViewModel newInstance(CustomerRepository repo) {
    return new AddressListViewModel(repo);
  }
}
