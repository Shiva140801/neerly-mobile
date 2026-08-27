package com.neerly.mobile.feature.wallet;

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
public final class WalletViewModel_Factory implements Factory<WalletViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public WalletViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public WalletViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static WalletViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new WalletViewModel_Factory(repoProvider);
  }

  public static WalletViewModel newInstance(CustomerRepository repo) {
    return new WalletViewModel(repo);
  }
}
