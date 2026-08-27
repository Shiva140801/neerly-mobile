package com.neerly.mobile.feature.address;

import androidx.lifecycle.SavedStateHandle;
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
public final class AddressFormViewModel_Factory implements Factory<AddressFormViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  private final Provider<SavedStateHandle> savedProvider;

  public AddressFormViewModel_Factory(Provider<CustomerRepository> repoProvider,
      Provider<SavedStateHandle> savedProvider) {
    this.repoProvider = repoProvider;
    this.savedProvider = savedProvider;
  }

  @Override
  public AddressFormViewModel get() {
    return newInstance(repoProvider.get(), savedProvider.get());
  }

  public static AddressFormViewModel_Factory create(Provider<CustomerRepository> repoProvider,
      Provider<SavedStateHandle> savedProvider) {
    return new AddressFormViewModel_Factory(repoProvider, savedProvider);
  }

  public static AddressFormViewModel newInstance(CustomerRepository repo, SavedStateHandle saved) {
    return new AddressFormViewModel(repo, saved);
  }
}
