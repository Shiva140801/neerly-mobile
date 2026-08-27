package com.neerly.mobile.feature.checkout;

import com.neerly.mobile.data.api.NeerlyApi;
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
public final class CheckoutViewModel_Factory implements Factory<CheckoutViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  private final Provider<NeerlyApi> apiProvider;

  private final Provider<CartStore> cartProvider;

  public CheckoutViewModel_Factory(Provider<CustomerRepository> repoProvider,
      Provider<NeerlyApi> apiProvider, Provider<CartStore> cartProvider) {
    this.repoProvider = repoProvider;
    this.apiProvider = apiProvider;
    this.cartProvider = cartProvider;
  }

  @Override
  public CheckoutViewModel get() {
    return newInstance(repoProvider.get(), apiProvider.get(), cartProvider.get());
  }

  public static CheckoutViewModel_Factory create(Provider<CustomerRepository> repoProvider,
      Provider<NeerlyApi> apiProvider, Provider<CartStore> cartProvider) {
    return new CheckoutViewModel_Factory(repoProvider, apiProvider, cartProvider);
  }

  public static CheckoutViewModel newInstance(CustomerRepository repo, NeerlyApi api,
      CartStore cart) {
    return new CheckoutViewModel(repo, api, cart);
  }
}
