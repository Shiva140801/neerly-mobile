package com.neerly.mobile.feature.customer;

import androidx.lifecycle.SavedStateHandle;
import com.neerly.mobile.data.cart.CartStore;
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
public final class VendorDetailViewModel_Factory implements Factory<VendorDetailViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  private final Provider<CartStore> cartProvider;

  private final Provider<SavedStateHandle> savedStateProvider;

  public VendorDetailViewModel_Factory(Provider<CustomerRepository> repoProvider,
      Provider<CartStore> cartProvider, Provider<SavedStateHandle> savedStateProvider) {
    this.repoProvider = repoProvider;
    this.cartProvider = cartProvider;
    this.savedStateProvider = savedStateProvider;
  }

  @Override
  public VendorDetailViewModel get() {
    return newInstance(repoProvider.get(), cartProvider.get(), savedStateProvider.get());
  }

  public static VendorDetailViewModel_Factory create(Provider<CustomerRepository> repoProvider,
      Provider<CartStore> cartProvider, Provider<SavedStateHandle> savedStateProvider) {
    return new VendorDetailViewModel_Factory(repoProvider, cartProvider, savedStateProvider);
  }

  public static VendorDetailViewModel newInstance(CustomerRepository repo, CartStore cart,
      SavedStateHandle savedState) {
    return new VendorDetailViewModel(repo, cart, savedState);
  }
}
